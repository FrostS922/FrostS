package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.SessionInfo;
import com.frosts.testplatform.entity.LoginHistory;
import com.frosts.testplatform.repository.LoginHistoryRepository;
import com.frosts.testplatform.repository.UserRepository;
import com.frosts.testplatform.service.LoginAnomalyAlertService;
import com.frosts.testplatform.service.SecurityWeeklyReportService;
import com.frosts.testplatform.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/security")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SecurityDashboardController {

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;
    private final LoginAnomalyAlertService loginAnomalyAlertService;
    private final SessionService sessionService;
    private final SecurityWeeklyReportService securityWeeklyReportService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityOverview() {
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

        Map<String, Object> overview = Map.of(
                "todayLoginSuccesses", todayLoginSuccesses,
                "todayLoginFailures", todayLoginFailures,
                "todayAnomalousIps", todayAnomalousIps,
                "lockedAccounts", lockedAccounts,
                "bannedIps", bannedIps
        );

        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/banned-ips")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getBannedIps() {
        List<Map<String, String>> bannedIps = loginAnomalyAlertService.getBannedIps();
        return ResponseEntity.ok(ApiResponse.success(bannedIps));
    }

    @DeleteMapping("/banned-ips/{ip}")
    public ResponseEntity<ApiResponse<Void>> unbanIp(@PathVariable String ip) {
        loginAnomalyAlertService.unbanIp(ip);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/login-trend")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLoginTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            List<LoginHistory> dayLogins = loginHistoryRepository.findByLoginAtBetween(dayStart, dayEnd);
            long successes = dayLogins.stream().filter(l -> l.getSuccess()).count();
            long failures = dayLogins.stream().filter(l -> !l.getSuccess()).count();

            Map<String, Object> point = new HashMap<>();
            point.put("date", date.toString());
            point.put("successes", successes);
            point.put("failures", failures);
            trend.add(point);
        }

        return ResponseEntity.ok(ApiResponse.success(trend));
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SessionInfo>>> getSessions(Authentication authentication) {
        String username = authentication.getName();
        List<SessionInfo> sessions = sessionService.getUserSessions(username, null);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping("/sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> terminateSession(
            Authentication authentication, @PathVariable Long id) {
        sessionService.terminateSession(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> terminateAllOtherSessions(Authentication authentication) {
        sessionService.terminateAllOtherSessions(authentication.getName(), null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/weekly-report/send")
    public ResponseEntity<ApiResponse<Void>> sendWeeklyReport() {
        securityWeeklyReportService.generateAndSendWeeklyReport();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/weekly-report/preview")
    public ResponseEntity<ApiResponse<String>> previewWeeklyReport() {
        String html = securityWeeklyReportService.generateReportHtml();
        return ResponseEntity.ok(ApiResponse.success(html));
    }
}
