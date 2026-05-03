# 安全增强功能设计文档

日期: 2026-05-02

## 概述

为 FrostS 测试平台新增三项安全增强功能：
1. 安全周报邮件 — 每周自动发送安全周报给管理员
2. 多因素认证 (MFA) — 为所有用户提供可选 TOTP 二次验证，管理员强制启用
3. 会话管理 — 基于现有 RefreshToken 的在线会话列表，支持强制下线

---

## 功能一：安全周报邮件

### 技术方案

引入 Thymeleaf 模板引擎生成 HTML 邮件，通过 @Scheduled 定时任务每周一 09:00 自动发送。

### 架构

```
@Scheduled (每周一 09:00)
    ↓
SecurityWeeklyReportService.generateWeeklyReport()
    ↓ 收集数据
├── LoginHistoryRepository → 登录统计
├── LoginAnomalyAlertService → 封禁IP
├── AuditLogRepository → 异常事件
└── UserRepository → 安全指标
    ↓
Thymeleaf 模板渲染 HTML
    ↓
JavaMailSender 发送给所有 ADMIN 用户
```

### 新增配置项

| 设置键 | 名称 | 默认值 | 分类 |
|--------|------|--------|------|
| `security.weekly_report.enabled` | 启用安全周报 | true | NOTIFICATION |
| `security.weekly_report.send_day` | 发送日(1=周一) | 1 | NOTIFICATION |
| `security.weekly_report.send_hour` | 发送时间(小时) | 9 | NOTIFICATION |

### 邮件内容

1. **登录统计**：本周登录成功/失败次数、日趋势、活跃用户 Top 5
2. **封禁情况**：当前被封禁 IP 列表、本周新增封禁数
3. **异常事件**：异常登录 IP、Token 滥用事件、暴力破解尝试
4. **安全指标**：MFA 启用率、密码策略合规率、在线会话数

### 新增类

| 类 | 职责 |
|----|------|
| `SecurityWeeklyReportService` | 数据收集、报告生成、邮件发送 |
| `SecurityWeeklyReportController` | 手动触发周报、预览周报 |

### 新增文件

| 文件 | 说明 |
|------|------|
| `resources/templates/email/security-weekly-report.html` | Thymeleaf 邮件模板 |

### 新增依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

---

## 功能二：多因素认证 (MFA/TOTP)

### 技术方案

使用 `dev.samstevens.totp:totp-spring-boot-starter` 库实现 RFC 6238 标准 TOTP，兼容 Google Authenticator、Microsoft Authenticator 等主流验证器 App。

### 架构

```
开启 MFA 流程:
  1. POST /auth/mfa/setup → 生成 TOTP Secret + 备份码
  2. 前端展示 QR Code (otpauth://totp/...)
  3. POST /auth/mfa/verify-setup → 输入验证码确认绑定

登录流程变更:
  POST /auth/login → 验证用户名密码
    ↓ 如果 mfaEnabled=true
  返回 { requireMfa: true, mfaToken: "临时token(5min)" }
    ↓ 前端跳转 MFA 验证页
  POST /auth/mfa/verify → 验证 TOTP 码/备份码
    ↓ 验证成功
  返回完整 JWT Token
```

### User 实体新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `mfaEnabled` | Boolean | 是否启用 MFA（默认 false） |
| `mfaSecret` | String(256) | TOTP 密钥（AES 加密存储） |
| `mfaBackupCodes` | String(1000) | 备份码（JSON 数组，BCrypt 哈希） |

### 安全设计

1. **管理员强制 MFA**：ADMIN 角色用户登录时，如果 `mfaEnabled=false`，强制跳转 MFA 设置页
2. **备份码**：生成 10 个一次性 8 位备份码，每个使用后从列表移除
3. **临时 MFA Token**：密码验证通过后发放短期 token（5 分钟有效），仅用于 MFA 验证端点
4. **加密存储**：`mfaSecret` 使用 AES 加密存储，密钥从 `app.mfa.encryption-key` 配置读取
5. **防暴力破解**：MFA 验证失败计入登录失败计数，触发 IP 封禁机制
6. **时间窗口容差**：允许 ±1 个时间步长（30秒）的验证码偏差

### 新增 DTO

| DTO | 字段 | 用途 |
|-----|------|------|
| `MfaSetupResponse` | secret, otpAuthUrl, backupCodes, qrCodeBase64 | MFA 设置信息 |
| `MfaVerifyRequest` | code, mfaToken | TOTP 验证码或备份码 |
| `MfaLoginResponse` | requireMfa, mfaToken | 登录时 MFA 状态 |

### 新增 API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/auth/mfa/setup` | 需登录 | 生成 TOTP Secret 和备份码 |
| POST | `/auth/mfa/verify-setup` | 需登录 | 确认 MFA 绑定 |
| POST | `/auth/mfa/verify` | mfaToken | 登录时验证 TOTP |
| DELETE | `/auth/mfa` | 需登录 | 关闭 MFA（需输入密码确认） |
| GET | `/auth/mfa/status` | 需登录 | 查询 MFA 状态 |

### 新增类

| 类 | 职责 |
|----|------|
| `MfaService` | TOTP Secret 生成、验证码校验、备份码管理 |
| 前端 `MfaSetupPage` | QR Code 展示、备份码下载 |
| 前端 `MfaVerifyPage` | 登录时 TOTP 验证码输入 |

### 新增依赖

```xml
<dependency>
    <groupId>dev.samstevens.totp</groupId>
    <artifactId>totp-spring-boot-starter</artifactId>
    <version>1.7.1</version>
</dependency>
```

### 新增配置

```yaml
app:
  mfa:
    issuer: FrostS
    encryption-key: ${MFA_ENCRYPTION_KEY:defaultMfaEncryptionKey2024}
```

---

## 功能三：会话管理

### 技术方案

扩展现有 `sys_refresh_token` 表，增加设备信息字段，登录时记录。基于 RefreshToken 实现会话追踪和强制下线。

### 架构

```
登录时:
  AuthService.login() → 创建 RefreshToken 时记录
    clientIp、userAgent、deviceInfo

会话列表:
  GET /security/sessions → 查询当前用户所有有效 RefreshToken
    返回: [{ id, device, ip, lastActive, createdAt, current }]

强制下线:
  DELETE /security/sessions/{tokenId} → 撤销指定 RefreshToken
    → 该设备下次刷新 Token 时失败，被迫重新登录

全部下线:
  DELETE /security/sessions → 撤销除当前会话外所有 RefreshToken
```

### RefreshToken 实体新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `clientIp` | String(50) | 登录 IP 地址 |
| `userAgent` | String(500) | 浏览器 User-Agent |
| `deviceInfo` | String(200) | 解析后的设备信息（如 "Chrome 120 / Windows 11"） |
| `lastRefreshedAt` | LocalDateTime | 最后刷新时间 |

### 设备信息解析

从 User-Agent 解析浏览器名称+版本、操作系统、设备类型。使用简单正则实现，避免引入额外依赖。

### 新增 API

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/security/sessions` | 需登录 | 当前用户的会话列表 |
| DELETE | `/security/sessions/{id}` | 需登录 | 强制下线指定会话 |
| DELETE | `/security/sessions` | 需登录 | 全部下线（除当前会话） |

### 新增 DTO

| DTO | 字段 | 用途 |
|-----|------|------|
| `SessionInfo` | id, device, ip, lastActive, createdAt, current | 会话信息 |

### 前端设计

在安全设置中新增「在线会话」标签页：
- 会话列表：设备图标 + 设备名 + IP + 最后活跃时间 + 当前会话标记
- 操作：强制下线（单个）、全部下线（除当前）

### 新增类

| 类 | 职责 |
|----|------|
| `SessionService` | 会话列表查询、强制下线、设备解析 |
| 前端 `SessionManagement` 组件 | 会话列表 UI |

---

## 数据库迁移

需要新增 Flyway 迁移脚本 `V6__add_security_enhancements.sql`：

```sql
-- User 表新增 MFA 字段
ALTER TABLE sys_user ADD COLUMN mfa_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE sys_user ADD COLUMN mfa_secret VARCHAR(256);
ALTER TABLE sys_user ADD COLUMN mfa_backup_codes VARCHAR(1000);

-- RefreshToken 表新增会话追踪字段
ALTER TABLE sys_refresh_token ADD COLUMN client_ip VARCHAR(50);
ALTER TABLE sys_refresh_token ADD COLUMN user_agent VARCHAR(500);
ALTER TABLE sys_refresh_token ADD COLUMN device_info VARCHAR(200);
ALTER TABLE sys_refresh_token ADD COLUMN last_refreshed_at TIMESTAMP;

-- 新增系统设置
INSERT INTO sys_system_setting (setting_key, setting_value, default_value, name, category, value_type, description, sort_order, is_deleted)
VALUES
('security.weekly_report.enabled', 'true', 'true', '启用安全周报', 'NOTIFICATION', 'BOOLEAN', '是否每周自动发送安全周报邮件', 0, false),
('security.weekly_report.send_day', '1', '1', '周报发送日', 'NOTIFICATION', 'NUMBER', '周报发送日(1=周一,7=周日)', 1, false),
('security.weekly_report.send_hour', '9', '9', '周报发送时间', 'NOTIFICATION', 'NUMBER', '周报发送时间(0-23时)', 2, false);
```

---

## 实施顺序

1. **数据库迁移** — 新增字段和配置项
2. **会话管理** — 改动最小，依赖最少，先实现
3. **MFA/TOTP** — 核心安全功能，涉及登录流程变更
4. **安全周报** — 依赖前两个功能的数据，最后实现
