# 消息通知中心系统设计文档

## 概述

为 FrostS 测试平台设计并实现功能完善的消息通知中心系统，支持系统公告、业务通知、用户提醒、待办催办四种消息类型，具备消息接收、存储、分类、已读/未读标记、删除等基础功能，提供直观的用户界面和消息推送机制，支持用户自定义通知偏好。

## 架构决策

### 方案选择：单体集成方案

在现有 Spring Boot 项目中直接新增通知模块，与现有架构完全一致。

**选择理由**：
1. 与现有架构一致 — 复用 BaseEntity、ApiResponse、Security 体系
2. 开发效率最高 — 无需额外基础设施
3. 测试平台通知量可控 — 轮询方案完全够用
4. 渐进式演进 — Spring Event 解耦触发逻辑，后续可升级 WebSocket
5. Redis 已就绪 — 后续升级时可直接利用现有 Redis 基础设施

### 推送机制：混合方案

- 第一阶段：前端轮询（10s 间隔）获取未读消息数量
- 预留接口：NotificationPushService 抽象推送层，后续实现 WebSocket 时只需替换实现类

## 数据模型

### 1. Notification（通知消息）

表名：`sys_notification`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| title | VARCHAR(200) | 通知标题 |
| content | TEXT | 通知内容 |
| type | VARCHAR(20) | 消息类型: SYSTEM / BUSINESS / REMINDER / TODO |
| category | VARCHAR(50) | 业务分类: DEFECT_ASSIGNED / PLAN_EXPIRED / ... |
| priority | VARCHAR(10) | 优先级: LOW / NORMAL / HIGH / URGENT |
| sender_id | BIGINT FK | 发送者ID（系统公告为管理员） |
| target_type | VARCHAR(30) | 关联目标类型: DEFECT / TEST_PLAN / PROJECT / ... |
| target_id | BIGINT | 关联目标ID |
| target_url | VARCHAR(500) | 点击跳转URL |
| is_global | BOOLEAN | 是否全局通知（系统公告） |
| expires_at | TIMESTAMP | 过期时间 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |
| created_by | VARCHAR | 创建者 |
| updated_by | VARCHAR | 更新者 |
| is_deleted | BOOLEAN | 逻辑删除 |

### 2. NotificationRecipient（通知接收关系）

表名：`sys_notification_recipient`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| notification_id | BIGINT FK | 通知ID |
| user_id | BIGINT FK | 接收用户ID |
| is_read | BOOLEAN | 是否已读（默认false） |
| read_at | TIMESTAMP | 阅读时间 |
| is_starred | BOOLEAN | 是否标星（默认false） |
| is_deleted | BOOLEAN | 用户侧删除（默认false） |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

### 3. NotificationPreference（通知偏好设置）

表名：`sys_notification_preference`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT FK UNIQUE | 用户ID（一对一） |
| type_settings | JSONB | 按类型开关 |
| category_settings | JSONB | 按分类开关 |
| receive_channels | JSONB | 接收渠道配置 |
| quiet_hours_start | TIME | 免打扰开始时间 |
| quiet_hours_end | TIME | 免打扰结束时间 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

### 设计要点

- Notification 与 Recipient 分离：一条通知可发给多人，避免内容冗余
- JSONB 存储偏好：PostgreSQL 原生支持，灵活扩展无需频繁改表
- is_global 字段：系统公告无需逐个创建 Recipient
- target_type + target_id：多态关联，支持跳转到任意业务实体

## 后端架构

### 模块结构

```
com.frosts.testplatform
├── entity/
│   ├── Notification.java
│   ├── NotificationRecipient.java
│   └── NotificationPreference.java
├── repository/
│   ├── NotificationRepository.java
│   ├── NotificationRecipientRepository.java
│   └── NotificationPreferenceRepository.java
├── dto/notification/
│   ├── NotificationResponse.java
│   ├── NotificationDetailResponse.java
│   ├── CreateNotificationRequest.java
│   ├── NotificationPreferenceResponse.java
│   ├── UpdateNotificationPreferenceRequest.java
│   └── UnreadCountResponse.java
├── event/
│   ├── NotificationEvent.java
│   └── NotificationEventListener.java
├── service/
│   ├── NotificationService.java
│   └── NotificationPreferenceService.java
└── controller/
    └── NotificationController.java
```

### API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/notifications` | 获取当前用户通知列表（分页，支持 type/category/is_read 筛选） |
| GET | `/notifications/unread-count` | 获取未读消息数量 |
| GET | `/notifications/{id}` | 获取通知详情 |
| PUT | `/notifications/{id}/read` | 标记单条已读 |
| PUT | `/notifications/read-all` | 全部标记已读 |
| PUT | `/notifications/{id}/star` | 标星/取消标星 |
| DELETE | `/notifications/{id}` | 删除通知（用户侧） |
| POST | `/notifications` | 创建通知（管理员/系统） |
| GET | `/notifications/preferences` | 获取通知偏好 |
| PUT | `/notifications/preferences` | 更新通知偏好 |

### Spring Event 触发机制

在业务 Service 中通过 ApplicationEventPublisher 发布 NotificationEvent，NotificationEventListener 监听并调用 NotificationService 创建通知，实现业务逻辑与通知逻辑解耦。

## 前端设计

### 组件结构

```
src/
├── api/notification.ts
├── store/notificationStore.ts
├── components/NotificationBell.tsx
├── pages/NotificationCenter.tsx
└── pages/NotificationSettings.tsx
```

### 界面布局

1. **通知铃铛**（Layout Header 右侧）
   - 铃铛图标 + 红色未读数 Badge
   - 点击弹出 Popover 显示最近 5 条未读通知
   - 底部「查看全部」跳转通知中心

2. **通知中心页面**（路由 `/notifications`）
   - Tab 切换：全部 / 系统公告 / 业务通知 / 用户提醒 / 待办催办
   - 筛选栏：已读/未读 + 时间范围 + 优先级
   - 操作栏：全部标记已读 + 批量删除
   - 消息列表：卡片式布局
   - 分页加载

3. **通知偏好设置页面**（路由 `/notifications/settings`）
   - 按类型开关
   - 按分类细控
   - 接收渠道配置
   - 免打扰时段设置

### 状态管理（Zustand）

notificationStore 管理 unreadCount、notifications、preferences 等状态，提供 fetchUnreadCount（轮询）、fetchNotifications、markAsRead、markAllAsRead、deleteNotification、fetchPreferences、updatePreferences 等方法。

### 路由与导航

- 侧边栏添加「通知中心」菜单项
- Header 右侧铃铛图标放在主题切换按钮之前
- 路由：`/notifications` 和 `/notifications/settings`

## 数据库迁移

新增 Flyway 迁移脚本 V4__add_notification_tables.sql，创建 sys_notification、sys_notification_recipient、sys_notification_preference 三张表。
