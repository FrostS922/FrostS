package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.security.LoginTrendPoint;
import com.frosts.testplatform.dto.security.SecurityOverviewResponse;
import com.frosts.testplatform.entity.LoginHistory;
import com.frosts.testplatform.repository.LoginHistoryRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SecurityDashboardService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;
    private final LoginAnomalyAlertService loginAnomalyAlertService;

    public SecurityOverviewResponse getSecurityOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        List<LoginHistory> todayLogins = loginHistoryRepository.findByLoginAtBetween(todayStart, LocalDateTime.now());
        long todayLoginFailures = todayLogins.stream().filter(l -> !l.getSuccess()).count();
        long todayLoginSuccesses = todayLogins.stream().filter(l -> l.getSuccess()).count();

        long todayAnomalousIps = todayLogins.stream()
                .filter(l -> !l.getSuccess())
                .map(LoginHistory::getLoginIp)
                .distinct()
                .count();

        long lockedAccounts = userRepository.countByAccountNonLockedFalseAndIsDeletedFalse();
        long bannedIps = loginAnomalyAlertService.getBannedIpCount();

        return SecurityOverviewResponse.builder()
                .todayLoginSuccesses(todayLoginSuccesses)
                .todayLoginFailures(todayLoginFailures)
                .todayAnomalousIps(todayAnomalousIps)
                .lockedAccounts(lockedAccounts)
                .bannedIps(bannedIps)
                .build();
    }

    public List<LoginTrendPoint> getLoginTrend() {
        List<LoginTrendPoint> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            List<LoginHistory> dayLogins = loginHistoryRepository.findByLoginAtBetween(dayStart, dayEnd);
            long successes = dayLogins.stream().filter(l -> l.getSuccess()).count();
            long failures = dayLogins.stream().filter(l -> !l.getSuccess()).count();

            trend.add(LoginTrendPoint.builder()
                    .date(date.toString())
                    .successes(successes)
                    .failures(failures)
                    .build());
        }

        return trend;
    }
}
