package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.security.LoginTrendPoint;
import com.frosts.testplatform.dto.security.SecurityOverviewResponse;
import com.frosts.testplatform.entity.LoginHistory;
import com.frosts.testplatform.repository.LoginHistoryRepository;
import com.frosts.testplatform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityDashboardServiceTest {

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAnomalyAlertService loginAnomalyAlertService;

    @InjectMocks
    private SecurityDashboardService securityDashboardService;

    @Test
    void getSecurityOverviewReturnsCorrectStats() {
        LoginHistory success1 = new LoginHistory();
        success1.setSuccess(true);
        success1.setLoginIp("192.168.1.1");
        LoginHistory success2 = new LoginHistory();
        success2.setSuccess(true);
        success2.setLoginIp("192.168.1.2");
        LoginHistory failure = new LoginHistory();
        failure.setSuccess(false);
        failure.setLoginIp("10.0.0.99");

        when(loginHistoryRepository.findByLoginAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(success1, success2, failure));
        when(userRepository.countByAccountNonLockedFalseAndIsDeletedFalse()).thenReturn(2L);
        when(loginAnomalyAlertService.getBannedIpCount()).thenReturn(3L);

        SecurityOverviewResponse overview = securityDashboardService.getSecurityOverview();

        assertThat(overview.getTodayLoginSuccesses()).isEqualTo(2L);
        assertThat(overview.getTodayLoginFailures()).isEqualTo(1L);
        assertThat(overview.getTodayAnomalousIps()).isEqualTo(1L);
        assertThat(overview.getLockedAccounts()).isEqualTo(2L);
        assertThat(overview.getBannedIps()).isEqualTo(3L);
    }

    @Test
    void getSecurityOverviewWithNoActivity() {
        when(loginHistoryRepository.findByLoginAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(userRepository.countByAccountNonLockedFalseAndIsDeletedFalse()).thenReturn(0L);
        when(loginAnomalyAlertService.getBannedIpCount()).thenReturn(0L);

        SecurityOverviewResponse overview = securityDashboardService.getSecurityOverview();

        assertThat(overview.getTodayLoginSuccesses()).isZero();
        assertThat(overview.getTodayLoginFailures()).isZero();
        assertThat(overview.getTodayAnomalousIps()).isZero();
        assertThat(overview.getLockedAccounts()).isZero();
        assertThat(overview.getBannedIps()).isZero();
    }

    @Test
    void getLoginTrendReturns7Days() {
        LoginHistory successLogin = new LoginHistory();
        successLogin.setSuccess(true);
        LoginHistory failLogin = new LoginHistory();
        failLogin.setSuccess(false);

        when(loginHistoryRepository.findByLoginAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(successLogin, failLogin));

        List<LoginTrendPoint> trend = securityDashboardService.getLoginTrend();

        assertThat(trend).hasSize(7);

        LocalDate today = LocalDate.now();
        assertThat(trend.get(0).getDate()).isEqualTo(today.minusDays(6).toString());
        assertThat(trend.get(6).getDate()).isEqualTo(today.toString());

        for (LoginTrendPoint point : trend) {
            assertThat(point.getSuccesses()).isEqualTo(1L);
            assertThat(point.getFailures()).isEqualTo(1L);
        }
    }

    @Test
    void getLoginTrendWithEmptyDays() {
        when(loginHistoryRepository.findByLoginAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<LoginTrendPoint> trend = securityDashboardService.getLoginTrend();

        assertThat(trend).hasSize(7);
        for (LoginTrendPoint point : trend) {
            assertThat(point.getSuccesses()).isZero();
            assertThat(point.getFailures()).isZero();
        }
    }

    @Test
    void getSecurityOverviewCountsDistinctAnomalousIps() {
        LoginHistory fail1 = new LoginHistory();
        fail1.setSuccess(false);
        fail1.setLoginIp("10.0.0.1");
        LoginHistory fail2 = new LoginHistory();
        fail2.setSuccess(false);
        fail2.setLoginIp("10.0.0.1");
        LoginHistory fail3 = new LoginHistory();
        fail3.setSuccess(false);
        fail3.setLoginIp("10.0.0.2");

        when(loginHistoryRepository.findByLoginAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(fail1, fail2, fail3));
        when(userRepository.countByAccountNonLockedFalseAndIsDeletedFalse()).thenReturn(0L);
        when(loginAnomalyAlertService.getBannedIpCount()).thenReturn(0L);

        SecurityOverviewResponse overview = securityDashboardService.getSecurityOverview();

        assertThat(overview.getTodayAnomalousIps()).isEqualTo(2L);
    }
}
