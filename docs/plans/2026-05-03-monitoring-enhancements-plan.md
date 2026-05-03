# FrostS 监控增强实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 FrostS 测试管理平台新增四个监控增强功能：实时大屏、告警规则配置、PDF 报告导出、APM 链路追踪

**Architecture:** 渐进式集成方案，在现有 Spring Boot + React 架构上扩展，复用 WebSocket/通知/Charts 基础设施，每个功能独立交付

**Tech Stack:** Spring Boot 3.2 / JPA / PostgreSQL / STOMP WebSocket / React 18 / TypeScript / Ant Design 5 / @ant-design/charts / html2canvas / jsPDF / SkyWalking 9.7

---

## 实施顺序

1. 模块 2: 告警规则可视化配置（后端 → 前端）— 基础设施层
2. 模块 1: 性能指标实时大屏（后端 → 前端）— 依赖告警规则
3. 模块 3: 监控数据导出 PDF 报告（后端 → 前端）— 依赖现有数据 API
4. 模块 4: APM 链路追踪集成（基础设施 → 后端 → 前端）— 独立模块

---

## 模块 2: 告警规则可视化配置

### Task 1: AlertRule 实体与数据库迁移

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/entity/AlertRule.java`
- Create: `backend/src/main/resources/db/migration/V7__add_alert_rule_table.sql`

**Step 1: 创建 AlertRule 实体**

```java
package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_alert_rule")
public class AlertRule extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String ruleType;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "metric_name", length = 50)
    private String metricName;

    @Column(name = "condition_type", length = 20)
    private String conditionType;

    private Double threshold;

    @Column(length = 10)
    private String comparator;

    @Column(name = "window_minutes")
    private Integer windowMinutes;

    @Column(name = "min_sample_count")
    private Integer minSampleCount;

    @Column(name = "notify_type", length = 20)
    private String notifyType;

    @Column(length = 20)
    private String priority;

    @Column(name = "cooldown_minutes")
    private Integer cooldownMinutes;

    @Column(columnDefinition = "TEXT")
    private String description;
}
```

**Step 2: 创建数据库迁移脚本**

```sql
CREATE TABLE sys_alert_rule (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    metric_name VARCHAR(50),
    condition_type VARCHAR(20),
    threshold DOUBLE PRECISION,
    comparator VARCHAR(10),
    window_minutes INTEGER,
    min_sample_count INTEGER,
    notify_type VARCHAR(20),
    priority VARCHAR(20),
    cooldown_minutes INTEGER,
    description TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

INSERT INTO sys_alert_rule (name, rule_type, enabled, metric_name, condition_type, threshold, comparator, window_minutes, min_sample_count, notify_type, priority, cooldown_minutes, description) VALUES
('LCP劣化告警', 'PERFORMANCE', TRUE, 'LCP', 'RATIO', 50.0, 'GTE', 30, 5, 'NOTIFICATION', 'HIGH', 60, 'LCP指标poor占比超过阈值时触发'),
('CLS劣化告警', 'PERFORMANCE', TRUE, 'CLS', 'RATIO', 50.0, 'GTE', 30, 5, 'NOTIFICATION', 'HIGH', 60, 'CLS指标poor占比超过阈值时触发'),
('FID劣化告警', 'PERFORMANCE', TRUE, 'FID', 'RATIO', 50.0, 'GTE', 30, 5, 'NOTIFICATION', 'MEDIUM', 60, 'FID指标poor占比超过阈值时触发'),
('TTFB劣化告警', 'PERFORMANCE', TRUE, 'TTFB', 'RATIO', 50.0, 'GTE', 30, 5, 'NOTIFICATION', 'HIGH', 60, 'TTFB指标poor占比超过阈值时触发'),
('错误率飙升', 'ERROR', TRUE, 'ERROR_RATE', 'THRESHOLD', 10.0, 'GTE', 30, 10, 'NOTIFICATION', 'CRITICAL', 30, '错误率超过阈值时触发'),
('异常IP告警', 'SECURITY', TRUE, 'ANOMALOUS_IP_COUNT', 'THRESHOLD', 5.0, 'GTE', 60, 1, 'NOTIFICATION', 'HIGH', 60, '异常IP数量超过阈值时触发');
```

**Step 3: 启动应用验证迁移**

Run: `cd backend && mvn spring-boot:run`
Expected: 应用正常启动，sys_alert_rule 表已创建

**Step 4: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/entity/AlertRule.java backend/src/main/resources/db/migration/V7__add_alert_rule_table.sql
git commit -m "feat: add AlertRule entity and database migration"
```

---

### Task 2: AlertRule Repository

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/repository/AlertRuleRepository.java`

**Step 1: 创建 AlertRuleRepository**

```java
package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    List<AlertRule> findByEnabledTrueAndIsDeletedFalse();

    List<AlertRule> findByRuleTypeAndEnabledTrueAndIsDeletedFalse(String ruleType);

    List<AlertRule> findByIsDeletedFalseOrderBySortOrderAscCreatedAtDesc();
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/repository/AlertRuleRepository.java
git commit -m "feat: add AlertRuleRepository"
```

---

### Task 3: AlertRule DTO

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/dto/alertrule/CreateAlertRuleRequest.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/alertrule/UpdateAlertRuleRequest.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/alertrule/AlertRuleResponse.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/alertrule/AlertRulePreviewResponse.java`

**Step 1: 创建 DTO 类**

CreateAlertRuleRequest:
```java
package com.frosts.testplatform.dto.alertrule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAlertRuleRequest {
    @NotBlank private String name;
    @NotBlank private String ruleType;
    private String metricName;
    private String conditionType;
    private Double threshold;
    private String comparator;
    private Integer windowMinutes;
    private Integer minSampleCount;
    private String notifyType;
    private String priority;
    private Integer cooldownMinutes;
    private String description;
}
```

UpdateAlertRuleRequest:
```java
package com.frosts.testplatform.dto.alertrule;

import lombok.Data;

@Data
public class UpdateAlertRuleRequest {
    private String name;
    private String ruleType;
    private Boolean enabled;
    private String metricName;
    private String conditionType;
    private Double threshold;
    private String comparator;
    private Integer windowMinutes;
    private Integer minSampleCount;
    private String notifyType;
    private String priority;
    private Integer cooldownMinutes;
    private String description;
}
```

AlertRuleResponse:
```java
package com.frosts.testplatform.dto.alertrule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleResponse {
    private Long id;
    private String name;
    private String ruleType;
    private Boolean enabled;
    private String metricName;
    private String conditionType;
    private Double threshold;
    private String comparator;
    private Integer windowMinutes;
    private Integer minSampleCount;
    private String notifyType;
    private String priority;
    private Integer cooldownMinutes;
    private String description;
    private String createdAt;
    private String updatedAt;
}
```

AlertRulePreviewResponse:
```java
package com.frosts.testplatform.dto.alertrule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRulePreviewResponse {
    private Double currentValue;
    private Double threshold;
    private Boolean wouldTrigger;
    private Long sampleCount;
    private String message;
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/dto/alertrule/
git commit -m "feat: add AlertRule DTOs"
```

---

### Task 4: AlertRuleService

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/service/AlertRuleService.java`

**Step 1: 创建 AlertRuleService**

```java
package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.alertrule.AlertRulePreviewResponse;
import com.frosts.testplatform.dto.alertrule.AlertRuleResponse;
import com.frosts.testplatform.dto.alertrule.CreateAlertRuleRequest;
import com.frosts.testplatform.dto.alertrule.UpdateAlertRuleRequest;
import com.frosts.testplatform.entity.AlertRule;
import com.frosts.testplatform.entity.ErrorLog;
import com.frosts.testplatform.entity.LoginHistory;
import com.frosts.testplatform.repository.AlertRuleRepository;
import com.frosts.testplatform.repository.ErrorLogRepository;
import com.frosts.testplatform.repository.LoginHistoryRepository;
import com.frosts.testplatform.repository.PerformanceLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final PerformanceLogRepository performanceLogRepository;
    private final ErrorLogRepository errorLogRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public List<AlertRuleResponse> getAllRules() {
        return alertRuleRepository.findByIsDeletedFalseOrderBySortOrderAscCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AlertRuleResponse createRule(CreateAlertRuleRequest request) {
        AlertRule rule = new AlertRule();
        rule.setName(request.getName());
        rule.setRuleType(request.getRuleType());
        rule.setMetricName(request.getMetricName());
        rule.setConditionType(request.getConditionType());
        rule.setThreshold(request.getThreshold());
        rule.setComparator(request.getComparator());
        rule.setWindowMinutes(request.getWindowMinutes());
        rule.setMinSampleCount(request.getMinSampleCount());
        rule.setNotifyType(request.getNotifyType());
        rule.setPriority(request.getPriority());
        rule.setCooldownMinutes(request.getCooldownMinutes());
        rule.setDescription(request.getDescription());
        rule.setEnabled(true);
        return toResponse(alertRuleRepository.save(rule));
    }

    @Transactional
    public AlertRuleResponse updateRule(Long id, UpdateAlertRuleRequest request) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));
        if (request.getName() != null) rule.setName(request.getName());
        if (request.getRuleType() != null) rule.setRuleType(request.getRuleType());
        if (request.getEnabled() != null) rule.setEnabled(request.getEnabled());
        if (request.getMetricName() != null) rule.setMetricName(request.getMetricName());
        if (request.getConditionType() != null) rule.setConditionType(request.getConditionType());
        if (request.getThreshold() != null) rule.setThreshold(request.getThreshold());
        if (request.getComparator() != null) rule.setComparator(request.getComparator());
        if (request.getWindowMinutes() != null) rule.setWindowMinutes(request.getWindowMinutes());
        if (request.getMinSampleCount() != null) rule.setMinSampleCount(request.getMinSampleCount());
        if (request.getNotifyType() != null) rule.setNotifyType(request.getNotifyType());
        if (request.getPriority() != null) rule.setPriority(request.getPriority());
        if (request.getCooldownMinutes() != null) rule.setCooldownMinutes(request.getCooldownMinutes());
        if (request.getDescription() != null) rule.setDescription(request.getDescription());
        return toResponse(alertRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));
        rule.setIsDeleted(true);
        alertRuleRepository.save(rule);
    }

    @Transactional
    public AlertRuleResponse toggleRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警规则不存在: " + id));
        rule.setEnabled(!rule.getEnabled());
        return toResponse(alertRuleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public AlertRulePreviewResponse previewRule(CreateAlertRuleRequest request) {
        int windowMinutes = request.getWindowMinutes() != null ? request.getWindowMinutes() : 30;
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);

        Double currentValue = getCurrentValue(request.getRuleType(), request.getMetricName(), since);
        Long sampleCount = getSampleCount(request.getRuleType(), request.getMetricName(), since);
        Double threshold = request.getThreshold() != null ? request.getThreshold() : 0.0;

        boolean wouldTrigger = false;
        String message;
        if (currentValue != null) {
            wouldTrigger = evaluateCondition(currentValue, threshold, request.getComparator());
            message = wouldTrigger
                    ? String.format("当前值 %.2f %s 阈值 %.2f，规则将会触发", currentValue, comparatorLabel(request.getComparator()), threshold)
                    : String.format("当前值 %.2f %s 阈值 %.2f，规则不会触发", currentValue, comparatorLabel(request.getComparator()), threshold);
        } else {
            message = "当前时段内无数据，无法预览";
        }

        return AlertRulePreviewResponse.builder()
                .currentValue(currentValue)
                .threshold(threshold)
                .wouldTrigger(wouldTrigger)
                .sampleCount(sampleCount)
                .message(message)
                .build();
    }

    private Double getCurrentValue(String ruleType, String metricName, LocalDateTime since) {
        if ("PERFORMANCE".equals(ruleType) && metricName != null) {
            long poorCount = performanceLogRepository.countByMetricNameAndRatingAndCreatedAtAfterAndIsDeletedFalse(metricName, "poor", since);
            long totalCount = performanceLogRepository.countByMetricNameAndCreatedAtAfterAndIsDeletedFalse(metricName, since);
            if (totalCount == 0) return null;
            return (poorCount * 100.0) / totalCount;
        }
        if ("ERROR".equals(ruleType)) {
            long errorCount = errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(since);
            return (double) errorCount;
        }
        if ("SECURITY".equals(ruleType) && "ANOMALOUS_IP_COUNT".equals(metricName)) {
            List<LoginHistory> failures = loginHistoryRepository.findByLoginAtBetween(since, LocalDateTime.now())
                    .stream().filter(l -> !l.getSuccess()).toList();
            return (double) failures.stream().map(LoginHistory::getLoginIp).distinct().count();
        }
        return null;
    }

    private Long getSampleCount(String ruleType, String metricName, LocalDateTime since) {
        if ("PERFORMANCE".equals(ruleType) && metricName != null) {
            return performanceLogRepository.countByMetricNameAndCreatedAtAfterAndIsDeletedFalse(metricName, since);
        }
        if ("ERROR".equals(ruleType)) {
            return errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(since);
        }
        return 0L;
    }

    private boolean evaluateCondition(Double value, Double threshold, String comparator) {
        if (comparator == null) comparator = "GTE";
        return switch (comparator) {
            case "GT" -> value > threshold;
            case "GTE" -> value >= threshold;
            case "LT" -> value < threshold;
            case "LTE" -> value <= threshold;
            case "EQ" -> value.equals(threshold);
            default -> value >= threshold;
        };
    }

    private String comparatorLabel(String comparator) {
        if (comparator == null) return ">=";
        return switch (comparator) {
            case "GT" -> ">";
            case "GTE" -> ">=";
            case "LT" -> "<";
            case "LTE" -> "<=";
            case "EQ" -> "=";
            default -> ">=";
        };
    }

    private AlertRuleResponse toResponse(AlertRule entity) {
        return AlertRuleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .ruleType(entity.getRuleType())
                .enabled(entity.getEnabled())
                .metricName(entity.getMetricName())
                .conditionType(entity.getConditionType())
                .threshold(entity.getThreshold())
                .comparator(entity.getComparator())
                .windowMinutes(entity.getWindowMinutes())
                .minSampleCount(entity.getMinSampleCount())
                .notifyType(entity.getNotifyType())
                .priority(entity.getPriority())
                .cooldownMinutes(entity.getCooldownMinutes())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(FMT) : "")
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().format(FMT) : "")
                .build();
    }
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/service/AlertRuleService.java
git commit -m "feat: add AlertRuleService with CRUD and preview"
```

---

### Task 5: AlertRuleController

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/controller/AlertRuleController.java`

**Step 1: 创建 AlertRuleController**

```java
package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.alertrule.AlertRulePreviewResponse;
import com.frosts.testplatform.dto.alertrule.AlertRuleResponse;
import com.frosts.testplatform.dto.alertrule.CreateAlertRuleRequest;
import com.frosts.testplatform.dto.alertrule.UpdateAlertRuleRequest;
import com.frosts.testplatform.service.AlertRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alert-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertRuleResponse>>> getAllRules() {
        return ResponseEntity.ok(ApiResponse.success(alertRuleService.getAllRules()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AlertRuleResponse>> createRule(@Valid @RequestBody CreateAlertRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(alertRuleService.createRule(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> updateRule(@PathVariable Long id, @RequestBody UpdateAlertRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(alertRuleService.updateRule(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> toggleRule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(alertRuleService.toggleRule(id)));
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<AlertRulePreviewResponse>> previewRule(@RequestBody CreateAlertRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(alertRuleService.previewRule(request)));
    }
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/controller/AlertRuleController.java
git commit -m "feat: add AlertRuleController"
```

---

### Task 6: 告警规则前端 API

**Files:**
- Create: `frontend/src/api/alertRule.ts`

**Step 1: 创建告警规则 API**

```typescript
import request from '@/utils/request'

export interface AlertRuleItem {
  id: number
  name: string
  ruleType: string
  enabled: boolean
  metricName: string | null
  conditionType: string | null
  threshold: number | null
  comparator: string | null
  windowMinutes: number | null
  minSampleCount: number | null
  notifyType: string | null
  priority: string | null
  cooldownMinutes: number | null
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface AlertRulePreview {
  currentValue: number | null
  threshold: number
  wouldTrigger: boolean
  sampleCount: number
  message: string
}

export interface CreateAlertRuleParams {
  name: string
  ruleType: string
  metricName?: string
  conditionType?: string
  threshold?: number
  comparator?: string
  windowMinutes?: number
  minSampleCount?: number
  notifyType?: string
  priority?: string
  cooldownMinutes?: number
  description?: string
}

export const getAlertRules = () => {
  return request.get('/alert-rules') as unknown as Promise<{
    code: number
    data: AlertRuleItem[]
  }>
}

export const createAlertRule = (data: CreateAlertRuleParams) => {
  return request.post('/alert-rules', data) as unknown as Promise<{
    code: number
    data: AlertRuleItem
  }>
}

export const updateAlertRule = (id: number, data: Partial<CreateAlertRuleParams>) => {
  return request.put(`/alert-rules/${id}`, data) as unknown as Promise<{
    code: number
    data: AlertRuleItem
  }>
}

export const deleteAlertRule = (id: number) => {
  return request.delete(`/alert-rules/${id}`) as unknown as Promise<{
    code: number
  }>
}

export const toggleAlertRule = (id: number) => {
  return request.patch(`/alert-rules/${id}/toggle`) as unknown as Promise<{
    code: number
    data: AlertRuleItem
  }>
}

export const previewAlertRule = (data: CreateAlertRuleParams) => {
  return request.post('/alert-rules/preview', data) as unknown as Promise<{
    code: number
    data: AlertRulePreview
  }>
}
```

**Step 2: Commit**

```bash
git add frontend/src/api/alertRule.ts
git commit -m "feat: add alert rule frontend API"
```

---

### Task 7: 告警规则前端 - SystemSettings 集成

**Files:**
- Modify: `frontend/src/pages/SystemSettings.tsx`

**Step 1: 在 SystemSettings 中新增告警规则 Tab**

在现有 SystemSettings 组件中添加告警规则管理功能：
- 新增 ALERT_RULE category 到 settingCategoryLabels
- 新增告警规则列表（Table + 启用/禁用 Switch）
- 新增规则编辑 Drawer（Slider 拖拽阈值 + 实时预览）
- 规则类型选择（PERFORMANCE / ERROR / SECURITY）
- 指标名称联动选择
- 阈值 Slider 配合 good/needs-improvement/poor 色带

**Step 2: Commit**

```bash
git add frontend/src/pages/SystemSettings.tsx
git commit -m "feat: integrate alert rule visual editor into SystemSettings"
```

---

## 模块 1: 性能指标实时大屏

### Task 8: 后端 WebSocket 实时推送

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/service/PerformanceReportService.java`
- Modify: `backend/src/main/java/com/frosts/testplatform/service/ErrorReportService.java`

**Step 1: 在 PerformanceReportService 中注入 SimpMessagingTemplate 并广播**

在 `reportPerformance()` 方法保存日志后，添加 WebSocket 广播：

```java
private final SimpMessagingTemplate messagingTemplate;

// 在 reportPerformance() 末尾添加:
Map<String, Object> payload = Map.of(
    "metricName", request.getName(),
    "metricValue", request.getValue(),
    "rating", request.getRating(),
    "timestamp", LocalDateTime.now().toString()
);
messagingTemplate.convertAndSend("/topic/perf-realtime", payload);
```

**Step 2: 在 ErrorReportService 中类似添加广播到 /topic/error-realtime**

**Step 3: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/service/PerformanceReportService.java backend/src/main/java/com/frosts/testplatform/service/ErrorReportService.java
git commit -m "feat: add WebSocket real-time broadcast for performance and error data"
```

---

### Task 9: MonitorDashboard 后端

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/dto/monitor/RealtimeSummaryResponse.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/monitor/RecentAlertResponse.java`
- Create: `backend/src/main/java/com/frosts/testplatform/controller/MonitorDashboardController.java`

**Step 1: 创建 RealtimeSummaryResponse**

聚合所有指标的实时快照，包含：性能概览、错误概览、安全概览、APM 指标占位。

**Step 2: 创建 RecentAlertResponse**

最近触发的告警列表，包含：告警时间、规则名称、指标、当前值、阈值。

**Step 3: 创建 MonitorDashboardController**

| 端点 | 方法 | 描述 |
|------|------|------|
| `/monitor/realtime-summary` | GET | 聚合实时快照 |
| `/monitor/alerts/recent` | GET | 最近告警 |

**Step 4: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/dto/monitor/ backend/src/main/java/com/frosts/testplatform/controller/MonitorDashboardController.java
git commit -m "feat: add MonitorDashboardController with realtime summary"
```

---

### Task 10: 前端监控 API

**Files:**
- Create: `frontend/src/api/monitor.ts`

**Step 1: 创建监控 API**

```typescript
import request from '@/utils/request'

export interface RealtimeSummary {
  performance: {
    totalReports: number
    todayReports: number
    metrics: { metricName: string; avgValue: number; poorCount: number; totalCount: number }[]
  }
  errors: {
    totalErrors: number
    todayErrors: number
    recentErrors: { errorMessage: string; category: string; createdAt: string }[]
  }
  security: {
    todayLoginSuccesses: number
    todayLoginFailures: number
    anomalousIps: number
    bannedIps: number
  }
  apm: {
    avgResponseTime: number | null
    p99: number | null
    throughput: number | null
    errorRate: number | null
  }
}

export interface RecentAlert {
  id: number
  ruleName: string
  metricName: string
  currentValue: number
  threshold: number
  triggeredAt: string
  priority: string
}

export const getRealtimeSummary = () => {
  return request.get('/monitor/realtime-summary') as unknown as Promise<{
    code: number
    data: RealtimeSummary
  }>
}

export const getRecentAlerts = () => {
  return request.get('/monitor/alerts/recent') as unknown as Promise<{
    code: number
    data: RecentAlert[]
  }>
}
```

**Step 2: Commit**

```bash
git add frontend/src/api/monitor.ts
git commit -m "feat: add monitor frontend API"
```

---

### Task 11: useMonitorWebSocket Hook

**Files:**
- Create: `frontend/src/hooks/useMonitorWebSocket.ts`

**Step 1: 创建 WebSocket 监控 Hook**

```typescript
import { useEffect, useRef, useState, useCallback } from 'react'
import { Client, IMessage } from '@stomp/stompjs'
import { getRealtimeSummary, type RealtimeSummary } from '@/api/monitor'

interface MonitorWebSocketState {
  connected: boolean
  performanceData: RealtimeSummary | null
  lastUpdate: Date | null
}

export const useMonitorWebSocket = (enabled: boolean = true) => {
  const [state, setState] = useState<MonitorWebSocketState>({
    connected: false,
    performanceData: null,
    lastUpdate: null,
  })
  const clientRef = useRef<Client | null>(null)
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const loadData = useCallback(async () => {
    try {
      const res = await getRealtimeSummary()
      if (res.code === 200 && res.data) {
        setState(prev => ({ ...prev, performanceData: res.data, lastUpdate: new Date() }))
      }
    } catch {}
  }, [])

  useEffect(() => {
    if (!enabled) return

    loadData()

    const client = new Client({
      brokerURL: `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws/notifications/websocket`,
      reconnectDelay: 5000,
      onConnect: () => {
        setState(prev => ({ ...prev, connected: true }))
        client.subscribe('/topic/perf-realtime', (msg: IMessage) => {
          loadData()
        })
        client.subscribe('/topic/error-realtime', (msg: IMessage) => {
          loadData()
        })
        client.subscribe('/topic/security-realtime', (msg: IMessage) => {
          loadData()
        })
      },
      onDisconnect: () => {
        setState(prev => ({ ...prev, connected: false }))
      },
    })

    try {
      client.activate()
      clientRef.current = client
    } catch {
      // fallback to polling
    }

    pollingRef.current = setInterval(loadData, 30000)

    return () => {
      client.deactivate()
      if (pollingRef.current) clearInterval(pollingRef.current)
    }
  }, [enabled, loadData])

  return state
}
```

**Step 2: Commit**

```bash
git add frontend/src/hooks/useMonitorWebSocket.ts
git commit -m "feat: add useMonitorWebSocket hook with polling fallback"
```

---

### Task 12: MonitorDashboard 全屏页面

**Files:**
- Create: `frontend/src/pages/MonitorDashboard.tsx`
- Modify: `frontend/src/App.tsx`

**Step 1: 创建 MonitorDashboard.tsx**

全屏深色主题大屏页面，包含：
- 6 个 Core Web Vitals Gauge 仪表盘
- 错误趋势折线图 + 安全指标卡片
- APM 链路指标面板
- 实时告警滚动条
- 右上角时间 + 连接状态
- F11 全屏支持

**Step 2: 在 App.tsx 中添加路由**

```tsx
<Route path="/monitor/dashboard" element={<MonitorDashboard />} />
```

注意：此路由在 Layout 外部，为独立全屏路由。

**Step 3: Commit**

```bash
git add frontend/src/pages/MonitorDashboard.tsx frontend/src/App.tsx
git commit -m "feat: add MonitorDashboard full-screen page"
```

---

### Task 13: Layout 导航添加监控入口

**Files:**
- Modify: `frontend/src/components/Layout.tsx`

**Step 1: 在菜单中添加监控大屏和 APM 链路入口**

```tsx
{
  key: '/monitor',
  icon: <DashboardOutlined />,
  label: '监控中心',
  children: [
    { key: '/monitor/dashboard', label: '实时大屏' },
    { key: '/monitor/tracing', label: '链路追踪' },
  ],
}
```

**Step 2: Commit**

```bash
git add frontend/src/components/Layout.tsx
git commit -m "feat: add monitor center menu entries"
```

---

## 模块 3: 监控数据导出 PDF 报告

### Task 14: PDF 报告后端

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/dto/report/ReportExportDataResponse.java`
- Create: `backend/src/main/java/com/frosts/testplatform/controller/ReportExportController.java`

**Step 1: 创建 ReportExportDataResponse**

聚合 DTO，包含：overview、trend、percentiles、diagnose、errorStats。

**Step 2: 创建 ReportExportController**

GET `/report/export-data?startTime=...&endTime=...` — 聚合所有报告数据。

**Step 3: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/dto/report/ backend/src/main/java/com/frosts/testplatform/controller/ReportExportController.java
git commit -m "feat: add ReportExportController with aggregated data endpoint"
```

---

### Task 15: 前端 PDF 依赖安装

**Step 1: 安装 html2canvas 和 jspdf**

Run: `cd frontend && pnpm add html2canvas jspdf`

**Step 2: Commit**

```bash
git add frontend/package.json frontend/pnpm-lock.yaml
git commit -m "chore: add html2canvas and jspdf dependencies"
```

---

### Task 16: ReportExportModal 组件

**Files:**
- Create: `frontend/src/components/ReportExportModal.tsx`

**Step 1: 创建报告导出 Modal**

包含：
- 报告标题输入
- 时间范围选择
- 包含模块勾选（性能概览/趋势图表/百分位分析/诊断结论/错误统计/APM链路）
- 生成进度条
- 导出按钮

**Step 2: Commit**

```bash
git add frontend/src/components/ReportExportModal.tsx
git commit -m "feat: add ReportExportModal component"
```

---

### Task 17: ReportPreview 组件

**Files:**
- Create: `frontend/src/components/ReportPreview.tsx`

**Step 1: 创建报告预览组件**

A4 比例布局，浅色主题，包含：
- 封面页
- 性能概览统计卡片
- Core Web Vitals 趋势折线图
- 百分位分析表格
- 错误统计图
- 诊断结论
- APM 链路概览

**Step 2: Commit**

```bash
git add frontend/src/components/ReportPreview.tsx
git commit -m "feat: add ReportPreview component for PDF rendering"
```

---

### Task 18: 集成 PDF 导出到 ErrorLogManagement

**Files:**
- Modify: `frontend/src/pages/ErrorLogManagement.tsx`

**Step 1: 在错误日志页面添加「导出 PDF 报告」按钮**

引入 ReportExportModal，添加导出按钮。

**Step 2: Commit**

```bash
git add frontend/src/pages/ErrorLogManagement.tsx
git commit -m "feat: integrate PDF export button into ErrorLogManagement"
```

---

## 模块 4: APM 链路追踪集成（SkyWalking）

### Task 19: Docker Compose 新增 SkyWalking

**Files:**
- Modify: `docker-compose.monitoring.yml`

**Step 1: 添加 SkyWalking OAP Server 和 UI 服务**

```yaml
skywalking-oap:
  image: apache/skywalking-oap-server:9.7.0
  container_name: frosts-skywalking-oap
  ports:
    - "11800:11800"
    - "12800:12800"
  environment:
    SW_STORAGE: elasticsearch
    SW_STORAGE_ES_CLUSTER_NODES: elasticsearch:9200
    SW_HEALTH_CHECKER: default
  depends_on:
    elasticsearch:
      condition: service_healthy
  healthcheck:
    test: ["CMD-SHELL", "curl -f http://localhost:12800/internal/l4check || exit 1"]
    interval: 15s
    timeout: 10s
    retries: 5

skywalking-ui:
  image: apache/skywalking-ui:9.7.0
  container_name: frosts-skywalking-ui
  ports:
    - "8080:8080"
  environment:
    SW_OAP_ADDRESS: http://skywalking-oap:12800
  depends_on:
    skywalking-oap:
      condition: service_healthy
```

**Step 2: Commit**

```bash
git add docker-compose.monitoring.yml
git commit -m "feat: add SkyWalking OAP and UI to monitoring docker-compose"
```

---

### Task 20: APM 后端 DTO 和 API Client

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/dto/apm/ServiceMetricResponse.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/apm/TraceListResponse.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/apm/TraceDetailResponse.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/apm/TopologyResponse.java`
- Create: `backend/src/main/java/com/frosts/testplatform/service/SkyWalkingApiClient.java`

**Step 1: 创建 APM DTO 类**

ServiceMetricResponse: serviceName, avgResponseTime, p99, throughput, errorRate, healthStatus

TraceListResponse: traceId, serviceName, endpoint, duration, spanCount, status, startTime

TraceDetailResponse: traceId, spans (树结构: spanId, parentSpanId, operationName, startTime, duration, status, tags)

TopologyResponse: nodes (服务节点), edges (调用关系)

**Step 2: 创建 SkyWalkingApiClient**

封装 SkyWalking GraphQL API 调用，使用 RestTemplate 发送 GraphQL 查询。

**Step 3: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/dto/apm/ backend/src/main/java/com/frosts/testplatform/service/SkyWalkingApiClient.java
git commit -m "feat: add APM DTOs and SkyWalking API client"
```

---

### Task 21: ApmController

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/controller/ApmController.java`

**Step 1: 创建 ApmController**

端点：
- GET `/apm/services` — 服务列表
- GET `/apm/service/{name}/metrics` — 服务指标
- GET `/apm/traces` — 链路查询
- GET `/apm/traces/{traceId}` — 链路详情
- GET `/apm/topology` — 服务拓扑

所有端点限 ADMIN 角色访问。

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/controller/ApmController.java
git commit -m "feat: add ApmController with service/trace/topology endpoints"
```

---

### Task 22: 前端 APM API

**Files:**
- Create: `frontend/src/api/apm.ts`

**Step 1: 创建 APM API**

```typescript
import request from '@/utils/request'

export interface ServiceMetric {
  serviceName: string
  avgResponseTime: number
  p99: number
  throughput: number
  errorRate: number
  healthStatus: string
}

export interface TraceItem {
  traceId: string
  serviceName: string
  endpoint: string
  duration: number
  spanCount: number
  status: string
  startTime: string
}

export interface TraceSpan {
  spanId: string
  parentSpanId: string
  operationName: string
  startTime: number
  duration: number
  status: string
  tags: Record<string, string>
}

export interface TopologyNode {
  name: string
  type: string
  healthStatus: string
}

export interface TopologyEdge {
  source: string
  target: string
  callCount: number
  errorRate: number
}

export const getApmServices = () => {
  return request.get('/apm/services') as unknown as Promise<{
    code: number
    data: ServiceMetric[]
  }>
}

export const getServiceMetrics = (serviceName: string, duration?: string) => {
  return request.get(`/apm/service/${encodeURIComponent(serviceName)}/metrics`, { params: { duration } }) as unknown as Promise<{
    code: number
    data: ServiceMetric
  }>
}

export const queryTraces = (params: {
  serviceName?: string
  startTime?: string
  endTime?: string
  minDuration?: number
  traceId?: string
}) => {
  return request.get('/apm/traces', { params }) as unknown as Promise<{
    code: number
    data: TraceItem[]
  }>
}

export const getTraceDetail = (traceId: string) => {
  return request.get(`/apm/traces/${traceId}`) as unknown as Promise<{
    code: number
    data: { traceId: string; spans: TraceSpan[] }
  }>
}

export const getServiceTopology = (duration?: string) => {
  return request.get('/apm/topology', { params: { duration } }) as unknown as Promise<{
    code: number
    data: { nodes: TopologyNode[]; edges: TopologyEdge[] }
  }>
}
```

**Step 2: Commit**

```bash
git add frontend/src/api/apm.ts
git commit -m "feat: add APM frontend API"
```

---

### Task 23: ApmTracing 页面

**Files:**
- Create: `frontend/src/pages/ApmTracing.tsx`
- Modify: `frontend/src/App.tsx`

**Step 1: 创建 ApmTracing.tsx**

四个 Tab 页面：
1. 服务列表 — 表格展示服务指标
2. 链路查询 — 搜索条件 + 结果表格
3. 链路详情 — Waterfall 瀑布图
4. 服务拓扑 — Graph 图展示调用关系

**Step 2: 在 App.tsx 中添加路由**

在 Layout 内部添加：
```tsx
<Route path="monitor/tracing" element={<ApmTracing />} />
```

**Step 3: Commit**

```bash
git add frontend/src/pages/ApmTracing.tsx frontend/src/App.tsx
git commit -m "feat: add ApmTracing page with service/trace/topology views"
```

---

### Task 24: 前端 Trace 关联

**Files:**
- Modify: `frontend/src/utils/request.ts`

**Step 1: 在 axios 响应拦截器中提取 traceId**

从响应头 `sw8` 或自定义 header 中提取 traceId，附加到错误上报和性能上报的 extra 字段中。

**Step 2: Commit**

```bash
git add frontend/src/utils/request.ts
git commit -m "feat: correlate frontend traces with backend APM traceId"
```

---

### Task 25: 集成测试与最终验证

**Step 1: 启动后端验证所有新端点**

Run: `cd backend && mvn spring-boot:run`

验证端点：
- GET /alert-rules
- POST /alert-rules
- GET /monitor/realtime-summary
- GET /monitor/alerts/recent
- GET /report/export-data
- GET /apm/services

**Step 2: 启动前端验证所有新页面**

Run: `cd frontend && pnpm dev`

验证页面：
- /monitor/dashboard — 实时大屏
- /system/settings — 告警规则 Tab
- /monitor/tracing — APM 链路追踪
- PDF 导出功能

**Step 3: 运行 lint 检查**

Run: `cd frontend && pnpm lint`

**Step 4: Final Commit**

```bash
git add -A
git commit -m "feat: complete monitoring enhancements - dashboard, alerts, PDF, APM"
```
