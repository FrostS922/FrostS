package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.alertrule.AlertRulePreviewResponse;
import com.frosts.testplatform.dto.alertrule.AlertRuleResponse;
import com.frosts.testplatform.dto.alertrule.CreateAlertRuleRequest;
import com.frosts.testplatform.dto.alertrule.UpdateAlertRuleRequest;
import com.frosts.testplatform.entity.AlertRule;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final PerformanceLogRepository performanceLogRepository;
    private final ErrorLogRepository errorLogRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public List<AlertRuleResponse> getAllRules() {
        return alertRuleRepository.findByIsDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AlertRuleResponse createRule(CreateAlertRuleRequest request) {
        AlertRule rule = new AlertRule();
        rule.setName(request.getName());
        rule.setRuleType(request.getRuleType());
        rule.setEnabled(true);
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
        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Created alert rule: id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public AlertRuleResponse updateRule(Long id, UpdateAlertRuleRequest request) {
        AlertRule rule = alertRuleRepository.findById(id)
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Alert rule not found: " + id));

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

        AlertRule updated = alertRuleRepository.save(rule);
        log.info("Updated alert rule: id={}", id);
        return toResponse(updated);
    }

    @Transactional
    public void deleteRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Alert rule not found: " + id));
        rule.setIsDeleted(true);
        alertRuleRepository.save(rule);
        log.info("Soft deleted alert rule: id={}", id);
    }

    @Transactional
    public AlertRuleResponse toggleRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Alert rule not found: " + id));
        rule.setEnabled(!rule.getEnabled());
        AlertRule toggled = alertRuleRepository.save(rule);
        log.info("Toggled alert rule: id={}, enabled={}", id, toggled.getEnabled());
        return toResponse(toggled);
    }

    public AlertRulePreviewResponse previewRule(CreateAlertRuleRequest request) {
        String ruleType = request.getRuleType();
        int windowMinutes = request.getWindowMinutes() != null ? request.getWindowMinutes() : 30;
        LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);

        switch (ruleType) {
            case "PERFORMANCE":
                return previewPerformanceRule(request, since);
            case "ERROR":
                return previewErrorRule(request, since);
            case "SECURITY":
                return previewSecurityRule(request, since);
            default:
                throw new RuntimeException("Unsupported rule type: " + ruleType);
        }
    }

    private AlertRulePreviewResponse previewPerformanceRule(CreateAlertRuleRequest request, LocalDateTime since) {
        String metricName = request.getMetricName();
        String conditionType = request.getConditionType() != null ? request.getConditionType() : "POOR_RATIO";

        long poorCount = performanceLogRepository.countByMetricNameAndRatingAndCreatedAtAfterAndIsDeletedFalse(metricName, "poor", since);
        long totalCount = performanceLogRepository.countByMetricNameAndCreatedAtAfterAndIsDeletedFalse(metricName, since);

        double currentValue = totalCount > 0 ? (poorCount * 100.0) / totalCount : 0.0;
        double threshold = request.getThreshold() != null ? request.getThreshold() : 50.0;
        String comparator = request.getComparator() != null ? request.getComparator() : "GTE";

        boolean wouldTrigger = evaluateComparator(currentValue, threshold, comparator);

        String message = String.format("性能指标[%s]在最近%d分钟内poor占比为%.2f%%，%s阈值%.2f%%，%s触发告警",
                metricName, request.getWindowMinutes() != null ? request.getWindowMinutes() : 30,
                currentValue, comparator, threshold, wouldTrigger ? "将会" : "不会");

        return AlertRulePreviewResponse.builder()
                .currentValue(currentValue)
                .threshold(threshold)
                .wouldTrigger(wouldTrigger)
                .sampleCount(totalCount)
                .message(message)
                .build();
    }

    private AlertRulePreviewResponse previewErrorRule(CreateAlertRuleRequest request, LocalDateTime since) {
        long errorCount = errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(since);
        double currentValue = (double) errorCount;
        double threshold = request.getThreshold() != null ? request.getThreshold() : 10.0;
        String comparator = request.getComparator() != null ? request.getComparator() : "GTE";

        boolean wouldTrigger = evaluateComparator(currentValue, threshold, comparator);

        String message = String.format("最近%d分钟内错误数为%d，%s阈值%.0f，%s触发告警",
                request.getWindowMinutes() != null ? request.getWindowMinutes() : 30,
                errorCount, comparator, threshold, wouldTrigger ? "将会" : "不会");

        return AlertRulePreviewResponse.builder()
                .currentValue(currentValue)
                .threshold(threshold)
                .wouldTrigger(wouldTrigger)
                .sampleCount(errorCount)
                .message(message)
                .build();
    }

    private AlertRulePreviewResponse previewSecurityRule(CreateAlertRuleRequest request, LocalDateTime since) {
        String conditionType = request.getConditionType() != null ? request.getConditionType() : "ANOMALOUS_IP_COUNT";

        if ("ANOMALOUS_IP_COUNT".equals(conditionType)) {
            LocalDateTime end = LocalDateTime.now();
            List<LoginHistory> failedLogins = loginHistoryRepository.findByLoginAtBetween(since, end).stream()
                    .filter(l -> !l.getSuccess())
                    .collect(Collectors.toList());

            long distinctIpCount = failedLogins.stream()
                    .map(LoginHistory::getLoginIp)
                    .distinct()
                    .count();

            double currentValue = (double) distinctIpCount;
            double threshold = request.getThreshold() != null ? request.getThreshold() : 5.0;
            String comparator = request.getComparator() != null ? request.getComparator() : "GTE";

            boolean wouldTrigger = evaluateComparator(currentValue, threshold, comparator);

            String message = String.format("最近%d分钟内异常登录IP数为%d，%s阈值%.0f，%s触发告警",
                    request.getWindowMinutes() != null ? request.getWindowMinutes() : 30,
                    distinctIpCount, comparator, threshold, wouldTrigger ? "将会" : "不会");

            return AlertRulePreviewResponse.builder()
                    .currentValue(currentValue)
                    .threshold(threshold)
                    .wouldTrigger(wouldTrigger)
                    .sampleCount((long) failedLogins.size())
                    .message(message)
                    .build();
        }

        throw new RuntimeException("Unsupported security condition type: " + conditionType);
    }

    private boolean evaluateComparator(double currentValue, double threshold, String comparator) {
        return switch (comparator) {
            case "GTE" -> currentValue >= threshold;
            case "GT" -> currentValue > threshold;
            case "LTE" -> currentValue <= threshold;
            case "LT" -> currentValue < threshold;
            case "EQ" -> currentValue == threshold;
            case "NEQ" -> currentValue != threshold;
            default -> currentValue >= threshold;
        };
    }

    private AlertRuleResponse toResponse(AlertRule rule) {
        return AlertRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .ruleType(rule.getRuleType())
                .enabled(rule.getEnabled())
                .metricName(rule.getMetricName())
                .conditionType(rule.getConditionType())
                .threshold(rule.getThreshold())
                .comparator(rule.getComparator())
                .windowMinutes(rule.getWindowMinutes())
                .minSampleCount(rule.getMinSampleCount())
                .notifyType(rule.getNotifyType())
                .priority(rule.getPriority())
                .cooldownMinutes(rule.getCooldownMinutes())
                .description(rule.getDescription())
                .createdAt(rule.getCreatedAt() != null ? rule.getCreatedAt().format(FORMATTER) : null)
                .updatedAt(rule.getUpdatedAt() != null ? rule.getUpdatedAt().format(FORMATTER) : null)
                .build();
    }
}
