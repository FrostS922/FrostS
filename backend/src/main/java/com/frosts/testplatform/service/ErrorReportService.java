package com.frosts.testplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frosts.testplatform.dto.errorreport.BatchDeleteErrorLogsRequest;
import com.frosts.testplatform.dto.errorreport.ErrorAggregationResponse;
import com.frosts.testplatform.dto.errorreport.ErrorLogResponse;
import com.frosts.testplatform.dto.errorreport.ErrorOverviewResponse;
import com.frosts.testplatform.dto.errorreport.ErrorReportRequest;
import com.frosts.testplatform.dto.errorreport.ErrorTrendResponse;
import com.frosts.testplatform.dto.notification.CreateNotificationRequest;
import com.frosts.testplatform.entity.ErrorLog;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.event.NotificationEvent;
import com.frosts.testplatform.repository.ErrorLogRepository;
import com.frosts.testplatform.repository.SystemSettingRepository;
import com.frosts.testplatform.repository.UserRepository;
import com.frosts.testplatform.service.push.NotificationPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorReportService {

    private final ErrorLogRepository errorLogRepository;
    private final UserRepository userRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final NotificationService notificationService;
    private final NotificationPushService notificationPushService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_ALERT_THRESHOLD = 5;
    private static final int DEFAULT_ALERT_WINDOW_MINUTES = 10;
    private static final int DEFAULT_ALERT_COOLDOWN_MINUTES = 30;
    private static final int DEFAULT_TREND_DAYS = 7;

    private final Map<String, LocalDateTime> lastAlertTime = new ConcurrentHashMap<>();

    @Transactional
    public void reportError(ErrorReportRequest request) {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setErrorMessage(truncate(request.getMessage(), 2000));
        errorLog.setStackTrace(truncate(request.getStack(), 10000));
        errorLog.setPageUrl(truncate(request.getUrl(), 500));
        errorLog.setUserAgent(truncate(request.getUserAgent(), 1000));
        errorLog.setCategory(classifyError(request.getMessage(), request.getStack()));

        if (request.getExtra() != null) {
            Integer status = extractInt(request.getExtra(), "status");
            String fallback = extractString(request.getExtra(), "fallback");
            errorLog.setHttpStatus(status);
            errorLog.setFallbackMessage(truncate(fallback, 500));

            request.getExtra().remove("status");
            request.getExtra().remove("fallback");
            if (!request.getExtra().isEmpty()) {
                errorLog.setExtraInfo(truncate(toJson(request.getExtra()), 5000));
            }
        }

        try {
            errorLogRepository.save(errorLog);
        } catch (Exception e) {
            log.error("保存错误日志失败: {}", e.getMessage());
            return;
        }

        pushNewError(errorLog);
        checkAndAlert(errorLog.getErrorMessage());
    }

    @Transactional(readOnly = true)
    public Page<ErrorLogResponse> getErrorLogs(String keyword, LocalDateTime startTime,
                                                LocalDateTime endTime, Pageable pageable) {
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        Page<ErrorLog> page = errorLogRepository.searchErrors(keyword, startTime, endTime, unsortedPageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public void deleteErrorLog(Long id) {
        ErrorLog errorLog = errorLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("错误日志不存在: " + id));
        errorLog.setIsDeleted(true);
        errorLogRepository.save(errorLog);
    }

    @Transactional
    public void batchDeleteErrorLogs(BatchDeleteErrorLogsRequest request) {
        List<ErrorLog> logs = errorLogRepository.findByIdInAndIsDeletedFalse(request.getIds());
        for (ErrorLog log : logs) {
            log.setIsDeleted(true);
        }
        errorLogRepository.saveAll(logs);
    }

    @Transactional(readOnly = true)
    public ErrorTrendResponse getErrorTrend(Integer days) {
        int trendDays = days != null && days > 0 ? days : DEFAULT_TREND_DAYS;
        LocalDateTime since = LocalDateTime.now().minusDays(trendDays).toLocalDate().atStartOfDay();

        List<Object[]> results = errorLogRepository.countByDate(since);
        Map<LocalDate, Long> countMap = results.stream()
                .collect(Collectors.toMap(
                        row -> ((java.sql.Date) row[0]).toLocalDate(),
                        row -> (Long) row[1]
                ));

        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = trendDays - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.format(formatter));
            counts.add(countMap.getOrDefault(date, 0L));
        }

        long total = errorLogRepository.countByIsDeletedFalse();
        return ErrorTrendResponse.builder()
                .dates(dates)
                .counts(counts)
                .total(total)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ErrorAggregationResponse> getAggregatedErrors(String keyword, LocalDateTime startTime,
                                                               LocalDateTime endTime, Pageable pageable) {
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        Page<Object[]> page = errorLogRepository.aggregateErrors(keyword, startTime, endTime, unsortedPageable);
        return page.map(row -> ErrorAggregationResponse.builder()
                .errorMessage((String) row[0])
                .count(((Number) row[1]).longValue())
                .lastSeen((LocalDateTime) row[2])
                .firstSeen((LocalDateTime) row[3])
                .build());
    }

    private void checkAndAlert(String errorMessage) {
        try {
            int threshold = getSettingAsInt("error_monitor.alert_threshold", DEFAULT_ALERT_THRESHOLD);
            int windowMinutes = getSettingAsInt("error_monitor.alert_window_minutes", DEFAULT_ALERT_WINDOW_MINUTES);
            int cooldownMinutes = getSettingAsInt("error_monitor.alert_cooldown_minutes", DEFAULT_ALERT_COOLDOWN_MINUTES);

            LocalDateTime since = LocalDateTime.now().minusMinutes(windowMinutes);
            long count = errorLogRepository.countByErrorMessageAndCreatedAtAfterAndIsDeletedFalse(errorMessage, since);

            if (count < threshold) return;

            String key = truncate(errorMessage, 100);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime last = lastAlertTime.get(key);
            if (last != null && last.isAfter(now.minusMinutes(cooldownMinutes))) return;

            lastAlertTime.put(key, now);

            String title = "前端错误告警";
            String content = String.format("错误「%s」在过去 %d 分钟内已出现 %d 次，超过阈值 %d 次",
                    truncate(errorMessage, 80), windowMinutes, count, threshold);

            List<Long> adminIds = userRepository.findByRoleCodeAndEnabled("ADMIN")
                    .stream().map(User::getId).toList();

            if (adminIds.isEmpty()) return;

            CreateNotificationRequest notifRequest = CreateNotificationRequest.builder()
                    .title(title)
                    .content(content)
                    .type("ALERT")
                    .category("ERROR_MONITOR")
                    .priority("HIGH")
                    .recipientIds(adminIds)
                    .targetType("ERROR_LOG")
                    .targetUrl("/error-logs")
                    .build();

            notificationService.createNotification(notifRequest);

            NotificationEvent event = NotificationEvent.builder()
                    .source(this)
                    .type("ALERT")
                    .category("ERROR_MONITOR")
                    .title(title)
                    .content(content)
                    .priority("HIGH")
                    .recipientIds(adminIds)
                    .targetType("ERROR_LOG")
                    .targetUrl("/error-logs")
                    .build();
            eventPublisher.publishEvent(event);

            log.warn("错误告警已触发: {}", content);
        } catch (Exception e) {
            log.error("错误告警检测失败: {}", e.getMessage());
        }
    }

    private int getSettingAsInt(String key, int defaultValue) {
        return systemSettingRepository.findBySettingKeyAndIsDeletedFalse(key)
                .map(s -> {
                    try { return Integer.parseInt(s.getSettingValue()); }
                    catch (NumberFormatException e) { return defaultValue; }
                })
                .orElse(defaultValue);
    }

    private ErrorLogResponse toResponse(ErrorLog entity) {
        return ErrorLogResponse.builder()
                .id(entity.getId())
                .errorMessage(entity.getErrorMessage())
                .stackTrace(entity.getStackTrace())
                .pageUrl(entity.getPageUrl())
                .userAgent(entity.getUserAgent())
                .httpStatus(entity.getHttpStatus())
                .fallbackMessage(entity.getFallbackMessage())
                .extraInfo(entity.getExtraInfo())
                .category(entity.getCategory())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }

    private String extractString(Map<String, Object> extra, String key) {
        Object value = extra.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer extractInt(Map<String, Object> extra, String key) {
        Object value = extra.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt((String) value); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (JsonProcessingException e) { return null; }
    }

    private void pushNewError(ErrorLog errorLog) {
        try {
            Map<String, Object> payload = Map.of(
                    "id", errorLog.getId(),
                    "errorMessage", truncate(errorLog.getErrorMessage(), 100),
                    "pageUrl", errorLog.getPageUrl() != null ? errorLog.getPageUrl() : "",
                    "httpStatus", errorLog.getHttpStatus() != null ? errorLog.getHttpStatus() : 0,
                    "category", errorLog.getCategory() != null ? errorLog.getCategory() : "",
                    "createdAt", errorLog.getCreatedAt().toString()
            );
            notificationPushService.pushToAll("NEW_ERROR_LOG", payload);
        } catch (Exception e) {
            log.debug("推送新错误通知失败: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<ErrorLogResponse> getErrorsByMessage(String errorMessage, Pageable pageable) {
        Page<ErrorLog> page = errorLogRepository.findByErrorMessageAndIsDeletedFalseOrderByCreatedAtDesc(errorMessage, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ErrorOverviewResponse getErrorOverview() {
        long total = errorLogRepository.countByIsDeletedFalse();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long today = errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);

        List<ErrorLog> recent = errorLogRepository.findTop5ByIsDeletedFalseOrderByCreatedAtDesc();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<ErrorOverviewResponse.RecentError> recentErrors = recent.stream()
                .map(log -> ErrorOverviewResponse.RecentError.builder()
                        .id(log.getId())
                        .errorMessage(truncate(log.getErrorMessage(), 80))
                        .httpStatus(log.getHttpStatus())
                        .pageUrl(log.getPageUrl())
                        .createdAt(log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : "")
                        .build())
                .toList();

        return ErrorOverviewResponse.builder()
                .totalErrors(total)
                .todayErrors(today)
                .recentErrors(recentErrors)
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportErrorLogs(String keyword, LocalDateTime startTime, LocalDateTime endTime, String format) {
        Pageable all = PageRequest.of(0, 10000);
        Page<ErrorLog> page = errorLogRepository.searchErrors(keyword, startTime, endTime, all);
        List<ErrorLog> logs = page.getContent();

        String[] headers = {"ID", "错误信息", "HTTP状态码", "页面URL", "兜底提示", "堆栈信息", "发生时间"};
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<String[]> rows = logs.stream().map(log -> new String[]{
                String.valueOf(log.getId()),
                log.getErrorMessage(),
                log.getHttpStatus() != null ? String.valueOf(log.getHttpStatus()) : "",
                log.getPageUrl(),
                log.getFallbackMessage(),
                truncate(log.getStackTrace(), 32000),
                log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : ""
        }).toList();

        if ("excel".equalsIgnoreCase(format)) {
            return com.frosts.testplatform.util.ExportUtils.toExcel(headers, rows);
        }
        return com.frosts.testplatform.util.ExportUtils.toCsv(headers, rows);
    }

    private String classifyError(String message, String stack) {
        if (message == null && stack == null) return "OTHER";
        String combined = (message != null ? message : "") + " " + (stack != null ? stack : "");
        String lower = combined.toLowerCase();

        if (lower.contains("network error") || lower.contains("fetch") || lower.contains("xhr") ||
                lower.contains("econnrefused") || lower.contains("enetunreach") || lower.contains("timeout"))
            return "NETWORK";
        if (lower.contains("chunkloaderror") || lower.contains("loading chunk") || lower.contains("loadjs") ||
                lower.contains("dynamically imported module") || lower.contains("import()"))
            return "RESOURCE";
        if (lower.contains("typeerror") || lower.contains("referenceerror") || lower.contains("syntaxerror") ||
                lower.contains("rangeerror") || lower.contains("cannot read propert"))
            return "CODE";
        if (lower.contains("unauthorized") || lower.contains("401") || lower.contains("forbidden") ||
                lower.contains("403") || lower.contains("token") || lower.contains("jwt"))
            return "AUTH";
        if (lower.contains("not found") || lower.contains("404"))
            return "NOT_FOUND";
        if (lower.contains("500") || lower.contains("502") || lower.contains("503") || lower.contains("504") ||
                lower.contains("internal server error"))
            return "SERVER";
        return "OTHER";
    }
}
