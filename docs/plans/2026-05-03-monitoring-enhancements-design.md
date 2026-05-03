# FrostS 监控增强设计文档

> 日期: 2026-05-03
> 架构方案: 渐进式集成（方案 A）

## 概述

本文档描述 FrostS 测试管理平台的四个监控增强功能的设计方案：

1. 性能指标实时大屏
2. 告警规则可视化配置
3. 监控数据导出 PDF 报告
4. APM 链路追踪集成（SkyWalking）

## 技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 大屏定位 | 独立全屏页面 | 专为投屏设计，无侧边栏干扰 |
| APM 工具 | SkyWalking 9.7 | 国产、功能全面、与 Spring Boot 无侵入集成 |
| PDF 生成 | 前端 html2canvas + jsPDF | 开发快，复用现有图表渲染 |
| 告警范围 | 多类型综合规则 | 覆盖性能+错误+安全，AND/OR 组合 |
| 实时刷新 | WebSocket 推送 | 复用现有 STOMP 基础设施 |
| 告警交互 | Slider 滑块拖拽 | 用户熟知，配合实时预览 |

---

## 模块 1: 性能指标实时大屏

### 路由

- 独立路由: `/monitor/dashboard`
- 不使用 Layout 组件，全屏展示
- 深色主题大屏风格

### 后端变更

#### 1.1 WebSocket 实时推送通道

在现有 `WebSocketConfig` 基础上新增 STOMP 目标：

- `/topic/perf-realtime` — 性能指标实时推送
- `/topic/error-realtime` — 错误日志实时推送
- `/topic/security-realtime` — 安全事件实时推送
- `/topic/apm-realtime` — APM 指标实时推送

#### 1.2 新增 MonitorDashboardController

| 端点 | 方法 | 描述 |
|------|------|------|
| `/monitor/realtime-summary` | GET | 聚合所有指标的实时快照（大屏初始加载） |
| `/monitor/alerts/recent` | GET | 最近触发的告警列表 |

#### 1.3 修改 PerformanceReportService

在 `reportPerformance()` 方法中，保存 PerformanceLog 后通过 `SimpMessagingTemplate` 向 `/topic/perf-realtime` 广播指标数据。

同样修改 `ErrorReportService` 和 `SecurityAlertService`，在关键事件发生时广播到对应 WebSocket 通道。

### 前端变更

#### 1.4 新增 MonitorDashboard.tsx

全屏页面，4 行布局：

| 行 | 内容 | 组件 |
|----|------|------|
| 第 1 行 | 6 个 Core Web Vitals 仪表盘 | Gauge 图（@ant-design/charts） |
| 第 2 行 | 错误趋势折线图 + 安全指标卡片 | Line + Statistic |
| 第 3 行 | APM 链路指标面板 | 折线图 + 数据表格 |
| 第 4 行 | 实时告警滚动条 | 滚动列表 |

特性：
- 自动刷新: WebSocket 实时推送 + 30s 轮询降级
- F11 全屏模式支持
- 右上角显示当前时间和连接状态
- 深色主题

#### 1.5 新增 useMonitorWebSocket Hook

封装 STOMP 订阅逻辑，支持：
- 自动重连
- 降级轮询
- 连接状态指示

### 数据流

```
前端上报性能指标 → POST /performance-report
  → PerformanceReportService.reportPerformance()
    → 保存 PerformanceLog
    → SimpMessagingTemplate.convertAndSend("/topic/perf-realtime", payload)
      → 大屏页面 useMonitorWebSocket 收到推送
        → 更新 Gauge/Line 组件
```

---

## 模块 2: 告警规则可视化配置

### 位置

系统设置页面新增「告警规则」Tab

### 后端变更

#### 2.1 新增 AlertRule 实体

```java
@Entity
@Table(name = "sys_alert_rule")
public class AlertRule extends BaseEntity {
    private String name;            // 规则名称
    private String ruleType;        // PERFORMANCE / ERROR / SECURITY
    private Boolean enabled;        // 是否启用
    private String metricName;      // 监控指标
    private String conditionType;   // THRESHOLD / RATIO / COUNT
    private Double threshold;       // 阈值
    private String comparator;      // GT / GTE / LT / LTE / EQ
    private Integer windowMinutes;  // 时间窗口（分钟）
    private Integer minSampleCount; // 最小样本数
    private String notifyType;      // NOTIFICATION / EMAIL / BOTH
    private String priority;        // LOW / MEDIUM / HIGH / CRITICAL
    private Integer cooldownMinutes;// 冷却时间
    private String description;     // 规则描述
}
```

#### 2.2 新增 AlertRuleController

| 端点 | 方法 | 描述 |
|------|------|------|
| `/alert-rules` | GET | 获取所有告警规则 |
| `/alert-rules` | POST | 创建告警规则 |
| `/alert-rules/{id}` | PUT | 更新告警规则 |
| `/alert-rules/{id}` | DELETE | 删除告警规则 |
| `/alert-rules/{id}/toggle` | PATCH | 启用/禁用规则 |
| `/alert-rules/preview` | POST | 预览规则触发效果 |

#### 2.3 新增 AlertRuleService

- `evaluateRule(AlertRule rule)` — 评估单条规则是否触发
- `evaluateAllRules()` — 评估所有启用的规则
- `previewRule(AlertRule rule)` — 预览规则效果

#### 2.4 重构 PerformanceReportService.checkAndAlert()

将硬编码告警逻辑改为从 AlertRule 表动态读取规则。

#### 2.5 数据库迁移 V7__add_alert_rule_table.sql

### 前端变更

#### 2.6 SystemSettings 新增告警规则 Tab

- 规则列表: Ant Design Table，支持启用/禁用开关
- 新增/编辑规则: Drawer 抽屉组件

#### 2.7 规则编辑器交互

- Slider 滑块拖拽调整阈值
- 配合 good/needs-improvement/poor 三段色带可视化
- 实时预览: 拖拽时调用 `/alert-rules/preview`，显示当前值 vs 阈值对比

#### 2.8 规则类型与指标映射

| 规则类型 | 可选指标 | 阈值单位 |
|---------|---------|---------|
| PERFORMANCE | LCP, CLS, FID, TTFB, INP, FCP | ms / 无(CLS) |
| ERROR | ERROR_RATE, ERROR_COUNT | % / 次 |
| SECURITY | ANOMALOUS_IP_COUNT, LOGIN_FAILURE_RATE | 次 / % |

---

## 模块 3: 监控数据导出 PDF 报告

### 触发入口

ErrorLogManagement / MonitorDashboard 页面的「导出 PDF 报告」按钮

### PDF 报告结构

1. 封面页 — 报告标题、时间范围、生成时间
2. 性能概览 — 总上报数、今日上报、各指标均值与评级
3. Core Web Vitals 趋势 — 折线图截图
4. 百分位分析 — P50/P75/P90/P95/P99 表格
5. 错误统计 — 错误分类饼图 + Top5 列表
6. 诊断结论 — 关联分析 / 结论 / 建议
7. APM 链路概览（可选）— 响应时间/吞吐量/错误率

### 后端变更

#### 3.1 新增 ReportExportController

| 端点 | 方法 | 描述 |
|------|------|------|
| `/report/export-data` | GET | 聚合所有报告所需数据 |

#### 3.2 新增 ReportExportDataResponse DTO

包含 overview、trend、percentiles、diagnose、error-stats 的聚合响应。

### 前端变更

#### 3.3 新增依赖

```json
{
  "html2canvas": "^1.4.1",
  "jspdf": "^2.5.1"
}
```

#### 3.4 新增 ReportExportModal 组件

- 报告配置表单: 标题、时间范围、包含模块勾选
- 生成进度条
- 预览功能

#### 3.5 新增 ReportPreview 组件

- A4 比例布局
- 浅色主题（PDF 推荐）
- 图表使用 @ant-design/charts 渲染

#### 3.6 导出流程

```
用户点击"导出 PDF"
  → 配置 Modal → 选择时间范围和包含模块
    → 调用 /report/export-data 获取聚合数据
      → 渲染 ReportPreview 组件（隐藏 DOM）
        → html2canvas 逐页截图 (scale=2)
          → jsPDF 合成多页 PDF
            → 浏览器下载
```

### 技术细节

- html2canvas scale=2 确保图表清晰
- 按模块分页，图表跨页自动断行
- 图片模式避免 jsPDF 中文字体问题
- 大数据量图表先聚合再渲染

---

## 模块 4: APM 链路追踪集成（SkyWalking）

### 基础设施

#### 4.1 Docker Compose 新增 SkyWalking

在 `docker-compose.monitoring.yml` 中新增:

- `skywalking-oap` — OAP Server (gRPC:11800, HTTP:12800)
- `skywalking-ui` — SkyWalking UI (HTTP:8080)
- 存储复用现有 Elasticsearch

#### 4.2 后端 Java Agent 配置

在 `docker-compose.yml` 的 backend 服务中:

- 挂载 skywalking-agent.jar
- 配置 SW_AGENT_NAME=frosts-backend
- 配置 SW_AGENT_COLLECTOR_BACKEND_SERVICES=skywalking-oap:11800

### 后端变更

#### 4.3 新增 ApmController

| 端点 | 方法 | 描述 |
|------|------|------|
| `/apm/services` | GET | 获取服务列表 |
| `/apm/service/{name}/metrics` | GET | 获取服务指标 |
| `/apm/traces` | GET | 查询链路列表 |
| `/apm/traces/{traceId}` | GET | 获取链路详情 |
| `/apm/topology` | GET | 获取服务拓扑图 |

所有端点限 ADMIN 角色访问。

#### 4.4 新增 SkyWalkingApiClient

封装对 SkyWalking GraphQL API 的调用:

- `getServiceMetrics(serviceName, duration)` — 服务指标
- `queryTraces(condition)` — 链路查询
- `getTraceDetail(traceId)` — 链路详情
- `getServiceTopology(duration)` — 服务拓扑

#### 4.5 新增 DTO

- ServiceMetricResponse — avgResponseTime, p99, throughput, errorRate
- TraceListResponse — 链路列表
- TraceDetailResponse — Span 树结构
- TopologyResponse — 节点 + 边

### 前端变更

#### 4.6 新增 ApmTracing.tsx

路由: `/monitor/tracing`

四个 Tab:

1. **服务列表** — 表格展示服务指标
2. **链路查询** — 搜索条件 + 结果表格
3. **链路详情** — Waterfall 瀑布图
4. **服务拓扑** — Graph 图展示调用关系

#### 4.7 前端 Trace 关联

在 `request.ts` axios 拦截器中:

- 从响应头读取 traceId
- 将 traceId 附加到错误上报和性能上报
- 实现前端错误 → 后端链路的关联

### 安全考虑

- APM API 仅限 ADMIN 角色
- SkyWalking UI 不直接暴露外网
- traceId 不包含敏感业务数据

---

## 路由规划

| 路由 | 页面 | Layout |
|------|------|--------|
| `/monitor/dashboard` | MonitorDashboard | 无（全屏独立） |
| `/monitor/tracing` | ApmTracing | Layout |

## 新增文件清单

### 后端

- `entity/AlertRule.java`
- `repository/AlertRuleRepository.java`
- `dto/alertrule/*.java` (CreateAlertRuleRequest, UpdateAlertRuleRequest, AlertRuleResponse, AlertRulePreviewResponse)
- `dto/monitor/RealtimeSummaryResponse.java`
- `dto/monitor/RecentAlertResponse.java`
- `dto/apm/*.java` (ServiceMetricResponse, TraceListResponse, TraceDetailResponse, TopologyResponse)
- `dto/report/ReportExportDataResponse.java`
- `controller/MonitorDashboardController.java`
- `controller/AlertRuleController.java`
- `controller/ApmController.java`
- `controller/ReportExportController.java`
- `service/AlertRuleService.java`
- `service/ApmService.java`
- `service/SkyWalkingApiClient.java`
- `db/migration/V7__add_alert_rule_table.sql`

### 前端

- `pages/MonitorDashboard.tsx`
- `pages/ApmTracing.tsx`
- `components/ReportExportModal.tsx`
- `components/ReportPreview.tsx`
- `hooks/useMonitorWebSocket.ts`
- `api/monitor.ts`
- `api/alertRule.ts`
- `api/apm.ts`

### 基础设施

- `docker-compose.monitoring.yml` (新增 SkyWalking 服务)
- `skywalking/agent/` (Java Agent 文件目录)
