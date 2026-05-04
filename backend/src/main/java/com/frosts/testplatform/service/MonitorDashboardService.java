package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.monitor.RealtimeSummaryResponse;
import com.frosts.testplatform.entity.ErrorLog;
import com.frosts.testplatform.entity.LoginHistory;
import com.frosts.testplatform.entity.Notification;
import com.frosts.testplatform.repository.ErrorLogRepository;
import com.frosts.testplatform.repository.LoginHistoryRepository;
import com.frosts.testplatform.repository.NotificationRepository;
import com.frosts.testplatform.repository.PerformanceLogRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonitorDashboardService {

    private final PerformanceLogRepository performanceLogRepository;
    private final ErrorLogRepository errorLogRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final LoginAnomalyAlertService loginAnomalyAlertService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public RealtimeSummaryResponse getRealtimeSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        RealtimeSummaryResponse.PerformanceSnapshot performance = buildPerformanceSnapshot(todayStart);
        RealtimeSummaryResponse.ErrorSnapshot errors = buildErrorSnapshot(todayStart);
        RealtimeSummaryResponse.SecuritySnapshot security = buildSecuritySnapshot(todayStart);

        return RealtimeSummaryResponse.builder()
                .performance(performance)
                .errors(errors)
                .security(security)
                .build();
    }

    public List<Notification> getRecentAlerts() {
        return notificationRepository.findRecentAlertsByCategories(
                List.of("PERF_MONITOR", "ERROR", "SECURITY"), PageRequest.of(0, 10));
    }

    private RealtimeSummaryResponse.PerformanceSnapshot buildPerformanceSnapshot(LocalDateTime todayStart) {
        long totalReports = performanceLogRepository.countByIsDeletedFalse();
        long todayReports = performanceLogRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> stats = performanceLogRepository.getMetricStats(since);

        List<RealtimeSummaryResponse.MetricSnapshot> metrics = stats.stream()
                .map(row -> RealtimeSummaryResponse.MetricSnapshot.builder()
                        .metricName((String) row[0])
                        .avgValue(Math.round(((Number) row[1]).doubleValue() * 100.0) / 100.0)
                        .count(((Number) row[4]).longValue())
                        .poorCount(((Number) row[5]).longValue())
                        .build())
                .toList();

        return RealtimeSummaryResponse.PerformanceSnapshot.builder()
                .totalReports(totalReports)
                .todayReports(todayReports)
                .metrics(metrics)
                .build();
    }

    private RealtimeSummaryResponse.ErrorSnapshot buildErrorSnapshot(LocalDateTime todayStart) {
        long totalErrors = errorLogRepository.countByIsDeletedFalse();
        long todayErrors = errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);

        List<ErrorLog> recentLogs = errorLogRepository.findTop5ByIsDeletedFalseOrderByCreatedAtDesc();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<RealtimeSummaryResponse.RecentError> recentErrors = recentLogs.stream()
                .map(log -> RealtimeSummaryResponse.RecentError.builder()
                        .errorMessage(truncate(log.getErrorMessage(), 80))
                        .category(log.getCategory() != null ? log.getCategory() : "")
                        .createdAt(log.getCreatedAt() != null ? log.getCreatedAt().format(fmt) : "")
                        .build())
                .toList();

        return RealtimeSummaryResponse.ErrorSnapshot.builder()
                .totalErrors(totalErrors)
                .todayErrors(todayErrors)
                .recentErrors(recentErrors)
                .build();
    }

    private RealtimeSummaryResponse.SecuritySnapshot buildSecuritySnapshot(LocalDateTime todayStart) {
        List<LoginHistory> todayLogins = loginHistoryRepository.findByLoginAtBetween(todayStart, LocalDateTime.now());
        long todayLoginSuccesses = todayLogins.stream().filter(l -> l.getSuccess()).count();
        long todayLoginFailures = todayLogins.stream().filter(l -> !l.getSuccess()).count();
        long todayAnomalousIps = todayLogins.stream()
                .filter(l -> !l.getSuccess())
                .map(LoginHistory::getLoginIp)
                .distinct()
                .count();
        long lockedAccounts = userRepository.countByAccountNonLockedFalseAndIsDeletedFalse();
        long bannedIps = loginAnomalyAlertService.getBannedIpCount();

        return RealtimeSummaryResponse.SecuritySnapshot.builder()
                .todayLoginSuccesses(todayLoginSuccesses)
                .todayLoginFailures(todayLoginFailures)
                .todayAnomalousIps(todayAnomalousIps)
                .lockedAccounts(lockedAccounts)
                .bannedIps(bannedIps)
                .build();
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }
}
