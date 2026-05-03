package com.frosts.testplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frosts.testplatform.dto.performancereport.DiagnosisResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceCompareResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceLogResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceOverviewResponse;
import com.frosts.testplatform.dto.performancereport.PerformancePercentileResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceReportRequest;
import com.frosts.testplatform.dto.performancereport.PerformanceTrendResponse;
import com.frosts.testplatform.dto.notification.CreateNotificationRequest;
import com.frosts.testplatform.entity.PerformanceLog;
import com.frosts.testplatform.entity.ErrorLog;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.event.NotificationEvent;
import com.frosts.testplatform.repository.PerformanceLogRepository;
import com.frosts.testplatform.repository.ErrorLogRepository;
import com.frosts.testplatform.repository.SystemSettingRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceReportService {

    private final PerformanceLogRepository performanceLogRepository;
    private final ErrorLogRepository errorLogRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int DEFAULT_PERF_ALERT_WINDOW_MINUTES = 30;
    private static final int DEFAULT_PERF_ALERT_POOR_RATIO = 50;
    private static final int DEFAULT_PERF_ALERT_COOLDOWN_MINUTES = 60;
    private static final int DEFAULT_PERF_MIN_SAMPLE_COUNT = 5;

    private static final Map<String, String> METRIC_LABELS = Map.of(
            "LCP", "最大内容绘制", "CLS", "累积布局偏移", "FID", "首次输入延迟",
            "TTFB", "首字节时间", "INP", "交互延迟", "FCP", "首次内容绘制"
    );

    private final Map<String, LocalDateTime> lastPerfAlertTime = new ConcurrentHashMap<>();

    @Transactional
    public void reportPerformance(PerformanceReportRequest request) {
        PerformanceLog perfLog = new PerformanceLog();
        perfLog.setMetricName(request.getName());
        perfLog.setMetricValue(request.getValue());
        perfLog.setRating(request.getRating());
        perfLog.setPageUrl(truncate(request.getUrl(), 500));
        perfLog.setUserAgent(truncate(request.getUserAgent(), 1000));

        if (request.getExtra() != null && !request.getExtra().isEmpty()) {
            perfLog.setExtraInfo(truncate(toJson(request.getExtra()), 5000));
        }

        try {
            performanceLogRepository.save(perfLog);
        } catch (Exception e) {
            log.error("保存性能指标失败: {}", e.getMessage());
            return;
        }

        if ("poor".equals(request.getRating())) {
            checkAndAlert(request.getName());
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("metricName", request.getName());
            payload.put("metricValue", request.getValue());
            payload.put("rating", request.getRating());
            payload.put("pageUrl", request.getUrl());
            payload.put("timestamp", LocalDateTime.now().toString());
            messagingTemplate.convertAndSend("/topic/perf-realtime", payload);
        } catch (Exception e) {
            log.error("WebSocket推送性能数据失败: {}", e.getMessage());
        }
    }

    private void checkAndAlert(String metricName) {
        try {
            int windowMinutes = getSettingAsInt("perf_monitor.alert_window_minutes", DEFAULT_PERF_ALERT_WINDOW_MINUTES);
            int poorRatioThreshold = getSettingAsInt("perf_monitor.alert_poor_ratio", DEFAULT_PERF_ALERT_POOR_RATIO);
            int cooldownMinutes = getSettingAsInt("perf_monitor.alert_cooldown_minutes", DEFAULT_PERF_ALERT_COOLDOWN_MINUTES);
            int minSampleCount = getSettingAsInt("perf_monitor.alert_min_sample_count", DEFAULT_PERF_MIN_SAMPLE_COUNT);

            LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
            long poorCount = performanceLogRepository.countByMetricNameAndRatingAndCreatedAtAfterAndIsDeletedFalse(metricName, "poor", since);
            long totalCount = performanceLogRepository.countByMetricNameAndCreatedAtAfterAndIsDeletedFalse(metricName, since);

            if (totalCount < minSampleCount) return;

            long poorRatio = (poorCount * 100) / totalCount;
            if (poorRatio < poorRatioThreshold) return;

            String key = "perf_" + metricName;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime last = lastPerfAlertTime.get(key);
            if (last != null && last.isAfter(now.minusMinutes(cooldownMinutes))) return;

            lastPerfAlertTime.put(key, now);

            String metricLabel = METRIC_LABELS.getOrDefault(metricName, metricName);
            String title = "性能指标告警";
            String content = String.format("指标「%s(%s)」在过去 %d 分钟内 poor 评级占比 %d%%（%d/%d），超过阈值 %d%%",
                    metricLabel, metricName, windowMinutes, poorRatio, poorCount, totalCount, poorRatioThreshold);

            List<Long> adminIds = userRepository.findByRoleCodeAndEnabled("ADMIN")
                    .stream().map(User::getId).toList();

            if (adminIds.isEmpty()) return;

            CreateNotificationRequest notifRequest = CreateNotificationRequest.builder()
                    .title(title)
                    .content(content)
                    .type("ALERT")
                    .category("PERF_MONITOR")
                    .priority("HIGH")
                    .recipientIds(adminIds)
                    .targetType("PERFORMANCE_LOG")
                    .targetUrl("/error-logs")
                    .build();

            notificationService.createNotification(notifRequest);

            NotificationEvent event = NotificationEvent.builder()
                    .source(this)
                    .type("ALERT")
                    .category("PERF_MONITOR")
                    .title(title)
                    .content(content)
                    .priority("HIGH")
                    .recipientIds(adminIds)
                    .targetType("PERFORMANCE_LOG")
                    .targetUrl("/error-logs")
                    .build();
            eventPublisher.publishEvent(event);

            log.warn("性能告警已触发: {}", content);
        } catch (Exception e) {
            log.error("性能告警检测失败: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<PerformanceLogResponse> getPerformanceLogs(String metricName, LocalDateTime startTime,
                                                           LocalDateTime endTime, Pageable pageable) {
        Page<PerformanceLog> page = performanceLogRepository.searchPerformanceLogs(metricName, startTime, endTime, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PerformanceOverviewResponse getPerformanceOverview() {
        long total = performanceLogRepository.countByIsDeletedFalse();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long today = performanceLogRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> stats = performanceLogRepository.getMetricStats(since);

        List<PerformanceOverviewResponse.MetricStat> metrics = stats.stream()
                .map(row -> PerformanceOverviewResponse.MetricStat.builder()
                        .metricName((String) row[0])
                        .avgValue(round2(((Number) row[1]).doubleValue()))
                        .minValue(round2(((Number) row[2]).doubleValue()))
                        .maxValue(round2(((Number) row[3]).doubleValue()))
                        .count(((Number) row[4]).longValue())
                        .poorCount(((Number) row[5]).longValue())
                        .build())
                .toList();

        return PerformanceOverviewResponse.builder()
                .totalReports(total)
                .todayReports(today)
                .metrics(metrics)
                .build();
    }

    @Transactional(readOnly = true)
    public PerformanceTrendResponse getPerformanceTrend(Integer days) {
        int trendDays = days != null && days > 0 ? days : DEFAULT_TREND_DAYS;
        LocalDateTime since = LocalDateTime.now().minusDays(trendDays).toLocalDate().atStartOfDay();

        List<Object[]> results = performanceLogRepository.getMetricTrend(since);

        Map<String, Map<LocalDate, Double>> metricDateMap = new LinkedHashMap<>();
        Set<LocalDate> allDates = new LinkedHashSet<>();

        for (Object[] row : results) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            String metricName = (String) row[1];
            Double avgValue = round2(((Number) row[2]).doubleValue());

            allDates.add(date);
            metricDateMap.computeIfAbsent(metricName, k -> new LinkedHashMap<>())
                    .put(date, avgValue);
        }

        List<LocalDate> sortedDates = allDates.stream().sorted().toList();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<String> dateStrings = sortedDates.stream().map(d -> d.format(fmt)).toList();

        List<PerformanceTrendResponse.MetricTrendPoint> metricPoints = metricDateMap.entrySet().stream()
                .map(entry -> {
                    List<Double> values = sortedDates.stream()
                            .map(d -> entry.getValue().getOrDefault(d, null))
                            .toList();
                    return PerformanceTrendResponse.MetricTrendPoint.builder()
                            .metricName(entry.getKey())
                            .values(values)
                            .build();
                })
                .toList();

        return PerformanceTrendResponse.builder()
                .dates(dateStrings)
                .metrics(metricPoints)
                .build();
    }

    @Transactional
    public void deletePerformanceLog(Long id) {
        PerformanceLog log = performanceLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("性能记录不存在: " + id));
        log.setIsDeleted(true);
        performanceLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PerformancePercentileResponse getPerformancePercentiles(Integer days) {
        int trendDays = days != null && days > 0 ? days : DEFAULT_TREND_DAYS;
        LocalDateTime since = LocalDateTime.now().minusDays(trendDays).toLocalDate().atStartOfDay();

        List<String> metricNames = performanceLogRepository.findDistinctMetricNamesSince(since);
        List<PerformancePercentileResponse.MetricPercentile> percentiles = new ArrayList<>();

        for (String metricName : metricNames) {
            List<Double> values = performanceLogRepository
                    .findValuesByMetricNameAndCreatedAtAfterAndIsDeletedFalse(metricName, since);

            if (values.isEmpty()) continue;

            percentiles.add(PerformancePercentileResponse.MetricPercentile.builder()
                    .metricName(metricName)
                    .p50(round2(percentile(values, 50)))
                    .p75(round2(percentile(values, 75)))
                    .p90(round2(percentile(values, 90)))
                    .p95(round2(percentile(values, 95)))
                    .p99(round2(percentile(values, 99)))
                    .sampleCount((long) values.size())
                    .build());
        }

        return PerformancePercentileResponse.builder()
                .metrics(percentiles)
                .build();
    }

    private double percentile(List<Double> sortedValues, int p) {
        if (sortedValues.isEmpty()) return 0;
        if (sortedValues.size() == 1) return sortedValues.get(0);
        double index = (p / 100.0) * (sortedValues.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) return sortedValues.get(lower);
        return sortedValues.get(lower) + (index - lower) * (sortedValues.get(upper) - sortedValues.get(lower));
    }

    @Transactional(readOnly = true)
    public PerformanceCompareResponse comparePerformance(LocalDateTime baseStart, LocalDateTime baseEnd,
                                                          LocalDateTime compareStart, LocalDateTime compareEnd) {
        List<Object[]> baseResults = performanceLogRepository.getMetricAvgBetween(baseStart, baseEnd);
        List<Object[]> compareResults = performanceLogRepository.getMetricAvgBetween(compareStart, compareEnd);

        Map<String, Object[]> baseMap = new LinkedHashMap<>();
        for (Object[] row : baseResults) {
            baseMap.put((String) row[0], row);
        }
        Map<String, Object[]> compareMap = new LinkedHashMap<>();
        for (Object[] row : compareResults) {
            compareMap.put((String) row[0], row);
        }

        Set<String> allMetrics = new LinkedHashSet<>();
        allMetrics.addAll(baseMap.keySet());
        allMetrics.addAll(compareMap.keySet());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        List<String> labels = List.of(
                baseStart.format(fmt) + " ~ " + baseEnd.format(fmt),
                compareStart.format(fmt) + " ~ " + compareEnd.format(fmt)
        );

        List<PerformanceCompareResponse.MetricCompare> metrics = allMetrics.stream()
                .map(metricName -> {
                    List<Double> avgValues = new ArrayList<>();
                    List<Long> sampleCounts = new ArrayList<>();

                    Object[] baseRow = baseMap.get(metricName);
                    avgValues.add(baseRow != null ? round2(((Number) baseRow[1]).doubleValue()) : null);
                    sampleCounts.add(baseRow != null ? ((Number) baseRow[2]).longValue() : 0L);

                    Object[] compareRow = compareMap.get(metricName);
                    avgValues.add(compareRow != null ? round2(((Number) compareRow[1]).doubleValue()) : null);
                    sampleCounts.add(compareRow != null ? ((Number) compareRow[2]).longValue() : 0L);

                    return PerformanceCompareResponse.MetricCompare.builder()
                            .metricName(metricName)
                            .avgValues(avgValues)
                            .sampleCounts(sampleCounts)
                            .build();
                })
                .toList();

        return PerformanceCompareResponse.builder()
                .labels(labels)
                .metrics(metrics)
                .build();
    }

    @Transactional(readOnly = true)
    public DiagnosisResponse diagnose(LocalDateTime startTime, LocalDateTime endTime) {
        List<ErrorLog> errors = errorLogRepository.findByTimeRange(startTime, endTime);
        List<Object[]> errorCategories = errorLogRepository.countByCategoryInRange(startTime, endTime);

        List<Object[]> perfStats = performanceLogRepository.getMetricAvgBetween(startTime, endTime);
        List<Object[]> allPerfStats = performanceLogRepository.getMetricStats(startTime);

        DiagnosisResponse.ErrorSummary errorSummary = DiagnosisResponse.ErrorSummary.builder()
                .totalErrors(errors.size())
                .topCategories(errorCategories.stream()
                        .limit(3)
                        .map(row -> (String) row[0] + "(" + ((Number) row[1]).longValue() + ")")
                        .toList())
                .topMessages(errors.stream()
                        .limit(5)
                        .map(e -> truncate(e.getErrorMessage(), 60))
                        .toList())
                .build();

        List<DiagnosisResponse.MetricIssue> poorMetrics = allPerfStats.stream()
                .filter(row -> ((Number) row[5]).longValue() > 0)
                .map(row -> DiagnosisResponse.MetricIssue.builder()
                        .metricName((String) row[0])
                        .avgValue(round2(((Number) row[1]).doubleValue()))
                        .poorCount(((Number) row[5]).longValue())
                        .totalCount(((Number) row[4]).longValue())
                        .build())
                .toList();

        DiagnosisResponse.PerformanceSummary perfSummary = DiagnosisResponse.PerformanceSummary.builder()
                .totalPoorMetrics(poorMetrics.stream().mapToLong(DiagnosisResponse.MetricIssue::getPoorCount).sum())
                .poorMetrics(poorMetrics)
                .build();

        List<String> correlations = new ArrayList<>();
        boolean hasNetworkErrors = errorCategories.stream()
                .anyMatch(row -> "NETWORK".equals(row[0]) && ((Number) row[1]).longValue() > 0);
        boolean hasResourceErrors = errorCategories.stream()
                .anyMatch(row -> "RESOURCE".equals(row[0]) && ((Number) row[1]).longValue() > 0);
        boolean hasServerErrors = errorCategories.stream()
                .anyMatch(row -> "SERVER".equals(row[0]) && ((Number) row[1]).longValue() > 0);
        boolean hasSlowTTFB = poorMetrics.stream()
                .anyMatch(m -> "TTFB".equals(m.getMetricName()) && m.getPoorCount() > 0);
        boolean hasSlowLCP = poorMetrics.stream()
                .anyMatch(m -> "LCP".equals(m.getMetricName()) && m.getPoorCount() > 0);
        boolean hasHighCLS = poorMetrics.stream()
                .anyMatch(m -> "CLS".equals(m.getMetricName()) && m.getPoorCount() > 0);

        if (hasNetworkErrors && hasSlowTTFB) {
            correlations.add("网络错误与 TTFB 劣化同时出现，可能存在网络连通性问题或服务端响应缓慢");
        }
        if (hasServerErrors && hasSlowTTFB) {
            correlations.add("服务端错误与 TTFB 劣化同时出现，后端服务可能过载或异常");
        }
        if (hasResourceErrors && hasSlowLCP) {
            correlations.add("资源加载错误与 LCP 劣化同时出现，静态资源可能加载失败导致页面渲染延迟");
        }
        if (hasNetworkErrors && hasSlowLCP) {
            correlations.add("网络错误与 LCP 劣化同时出现，网络问题可能影响了关键资源的加载");
        }
        if (hasHighCLS && errors.size() > 3) {
            correlations.add("高 CLS 与频繁错误同时出现，错误提示或弹窗可能导致页面布局不稳定");
        }
        if (hasResourceErrors && hasHighCLS) {
            correlations.add("资源加载错误与 CLS 劣化同时出现，资源加载失败可能导致布局偏移");
        }

        String conclusion;
        if (correlations.isEmpty() && errors.isEmpty() && poorMetrics.isEmpty()) {
            conclusion = "该时段内系统运行正常，未发现错误和性能问题。";
        } else if (correlations.isEmpty()) {
            conclusion = errors.isEmpty()
                    ? String.format("该时段内存在 %d 个性能指标劣化，但未发现与错误的直接关联。", poorMetrics.size())
                    : String.format("该时段内存在 %d 个错误，但未发现与性能劣化的直接关联。", errors.size());
        } else {
            conclusion = String.format("发现 %d 条关联线索，错误与性能问题可能存在因果关系，建议优先排查：%s",
                    correlations.size(), correlations.get(0));
        }

        return DiagnosisResponse.builder()
                .startTime(startTime)
                .endTime(endTime)
                .errorSummary(errorSummary)
                .performanceSummary(perfSummary)
                .correlations(correlations)
                .conclusion(conclusion)
                .build();
    }

    private PerformanceLogResponse toResponse(PerformanceLog entity) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return PerformanceLogResponse.builder()
                .id(entity.getId())
                .metricName(entity.getMetricName())
                .metricValue(round2(entity.getMetricValue()))
                .rating(entity.getRating())
                .pageUrl(entity.getPageUrl())
                .userAgent(entity.getUserAgent())
                .extraInfo(entity.getExtraInfo())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(fmt) : "")
                .build();
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (JsonProcessingException e) { return null; }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private int getSettingAsInt(String key, int defaultValue) {
        return systemSettingRepository.findBySettingKeyAndIsDeletedFalse(key)
                .map(s -> {
                    try { return Integer.parseInt(s.getSettingValue()); }
                    catch (NumberFormatException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }
}
