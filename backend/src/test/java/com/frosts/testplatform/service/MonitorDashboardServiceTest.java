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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorDashboardServiceTest {

    @Mock
    private PerformanceLogRepository performanceLogRepository;

    @Mock
    private ErrorLogRepository errorLogRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private LoginAnomalyAlertService loginAnomalyAlertService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MonitorDashboardService monitorDashboardService;

    @Test
    void getRealtimeSummaryReturnsAllSnapshots() {
        when(performanceLogRepository.countByIsDeletedFalse()).thenReturn(100L);
        when(performanceLogRepository.countByCreatedAtAfterAndIsDeletedFalse(any(LocalDateTime.class))).thenReturn(10L);
        Object[] metricRow = new Object[]{"LCP", 2.5, 0.8, 8.0, 50L, 3L};
        List<Object[]> metricStats = new java.util.ArrayList<>();
        metricStats.add(metricRow);
        when(performanceLogRepository.getMetricStats(any(LocalDateTime.class))).thenReturn(metricStats);

        when(errorLogRepository.countByIsDeletedFalse()).thenReturn(20L);
        when(errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(any(LocalDateTime.class))).thenReturn(2L);

        ErrorLog errorLog = new ErrorLog();
        errorLog.setErrorMessage("TypeError: Cannot read properties of undefined");
        errorLog.setCategory("CODE");
        errorLog.setCreatedAt(LocalDateTime.of(2026, 5, 4, 9, 30, 0));
        when(errorLogRepository.findTop5ByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(errorLog));

        LoginHistory successLogin = new LoginHistory();
        successLogin.setSuccess(true);
        successLogin.setLoginIp("192.168.1.1");
        LoginHistory failLogin = new LoginHistory();
        failLogin.setSuccess(false);
        failLogin.setLoginIp("10.0.0.99");
        when(loginHistoryRepository.findByLoginAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(successLogin, failLogin));
        when(loginAnomalyAlertService.getBannedIpCount()).thenReturn(1L);
        when(userRepository.countByAccountNonLockedFalseAndIsDeletedFalse()).thenReturn(2L);

        RealtimeSummaryResponse response = monitorDashboardService.getRealtimeSummary();

        assertThat(response.getPerformance()).isNotNull();
        assertThat(response.getPerformance().getTotalReports()).isEqualTo(100L);
        assertThat(response.getPerformance().getTodayReports()).isEqualTo(10L);
        assertThat(response.getPerformance().getMetrics()).hasSize(1);
        assertThat(response.getPerformance().getMetrics().get(0).getMetricName()).isEqualTo("LCP");

        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors().getTotalErrors()).isEqualTo(20L);
        assertThat(response.getErrors().getTodayErrors()).isEqualTo(2L);
        assertThat(response.getErrors().getRecentErrors()).hasSize(1);
        assertThat(response.getErrors().getRecentErrors().get(0).getCategory()).isEqualTo("CODE");

        assertThat(response.getSecurity()).isNotNull();
        assertThat(response.getSecurity().getTodayLoginSuccesses()).isEqualTo(1L);
        assertThat(response.getSecurity().getTodayLoginFailures()).isEqualTo(1L);
        assertThat(response.getSecurity().getTodayAnomalousIps()).isEqualTo(1L);
        assertThat(response.getSecurity().getLockedAccounts()).isEqualTo(2L);
        assertThat(response.getSecurity().getBannedIps()).isEqualTo(1L);
    }

    @Test
    void getRealtimeSummaryWithEmptyData() {
        when(performanceLogRepository.countByIsDeletedFalse()).thenReturn(0L);
        when(performanceLogRepository.countByCreatedAtAfterAndIsDeletedFalse(any(LocalDateTime.class))).thenReturn(0L);
        when(performanceLogRepository.getMetricStats(any(LocalDateTime.class))).thenReturn(List.of());
        when(errorLogRepository.countByIsDeletedFalse()).thenReturn(0L);
        when(errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(any(LocalDateTime.class))).thenReturn(0L);
        when(errorLogRepository.findTop5ByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of());
        when(loginHistoryRepository.findByLoginAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(loginAnomalyAlertService.getBannedIpCount()).thenReturn(0L);
        when(userRepository.countByAccountNonLockedFalseAndIsDeletedFalse()).thenReturn(0L);

        RealtimeSummaryResponse response = monitorDashboardService.getRealtimeSummary();

        assertThat(response.getPerformance().getTotalReports()).isZero();
        assertThat(response.getPerformance().getMetrics()).isEmpty();
        assertThat(response.getErrors().getRecentErrors()).isEmpty();
        assertThat(response.getSecurity().getTodayLoginSuccesses()).isZero();
        assertThat(response.getSecurity().getLockedAccounts()).isZero();
    }

    @Test
    void getRecentAlertsReturnsNotificationsFromRepository() {
        Notification notif = new Notification();
        notif.setId(1L);
        notif.setTitle("性能告警");
        notif.setType("ALERT");
        notif.setCategory("PERF_MONITOR");

        when(notificationRepository.findRecentAlertsByCategories(
                eq(List.of("PERF_MONITOR", "ERROR", "SECURITY")), any(PageRequest.class)))
                .thenReturn(List.of(notif));

        List<Notification> alerts = monitorDashboardService.getRecentAlerts();

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getTitle()).isEqualTo("性能告警");
    }

    @Test
    void getRecentAlertsReturnsEmptyWhenNoAlerts() {
        when(notificationRepository.findRecentAlertsByCategories(
                eq(List.of("PERF_MONITOR", "ERROR", "SECURITY")), any(PageRequest.class)))
                .thenReturn(List.of());

        List<Notification> alerts = monitorDashboardService.getRecentAlerts();

        assertThat(alerts).isEmpty();
    }

    @Test
    void errorSnapshotTruncatesLongErrorMessage() {
        when(performanceLogRepository.countByIsDeletedFalse()).thenReturn(0L);
        when(performanceLogRepository.countByCreatedAtAfterAndIsDeletedFalse(any(LocalDateTime.class))).thenReturn(0L);
        when(performanceLogRepository.getMetricStats(any(LocalDateTime.class))).thenReturn(List.of());
        when(errorLogRepository.countByIsDeletedFalse()).thenReturn(0L);
        when(errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(any(LocalDateTime.class))).thenReturn(0L);

        ErrorLog longErrorLog = new ErrorLog();
        longErrorLog.setErrorMessage("A".repeat(200));
        longErrorLog.setCategory("NETWORK");
        longErrorLog.setCreatedAt(LocalDateTime.now());
        when(errorLogRepository.findTop5ByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(longErrorLog));

        when(loginHistoryRepository.findByLoginAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(loginAnomalyAlertService.getBannedIpCount()).thenReturn(0L);
        when(userRepository.countByAccountNonLockedFalseAndIsDeletedFalse()).thenReturn(0L);

        RealtimeSummaryResponse response = monitorDashboardService.getRealtimeSummary();

        assertThat(response.getErrors().getRecentErrors().get(0).getErrorMessage()).hasSize(80);
    }
}
