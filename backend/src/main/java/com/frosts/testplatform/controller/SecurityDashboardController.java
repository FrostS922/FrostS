package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.SessionInfo;
import com.frosts.testplatform.dto.security.LoginTrendPoint;
import com.frosts.testplatform.dto.security.SecurityOverviewResponse;
import com.frosts.testplatform.service.LoginAnomalyAlertService;
import com.frosts.testplatform.service.SecurityDashboardService;
import com.frosts.testplatform.service.SecurityWeeklyReportService;
import com.frosts.testplatform.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/security")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "安全仪表盘", description = "安全概览、IP封禁管理、登录趋势、会话管理与周报")
public class SecurityDashboardController {

    private final SecurityDashboardService securityDashboardService;
    private final LoginAnomalyAlertService loginAnomalyAlertService;
    private final SessionService sessionService;
    private final SecurityWeeklyReportService securityWeeklyReportService;

    @GetMapping("/overview")
    @Operation(summary = "获取安全概览", description = "返回今日登录统计、异常IP数、锁定账户数和封禁IP数")
    public ResponseEntity<ApiResponse<SecurityOverviewResponse>> getSecurityOverview() {
        SecurityOverviewResponse overview = securityDashboardService.getSecurityOverview();
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/banned-ips")
    @Operation(summary = "获取封禁IP列表", description = "返回当前所有被封禁的IP地址及其封禁时间和剩余秒数")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getBannedIps() {
        List<Map<String, String>> bannedIps = loginAnomalyAlertService.getBannedIps();
        return ResponseEntity.ok(ApiResponse.success(bannedIps));
    }

    @DeleteMapping("/banned-ips/{ip}")
    @Operation(summary = "解封IP", description = "解除指定IP的封禁状态")
    public ResponseEntity<ApiResponse<Void>> unbanIp(
            @Parameter(description = "需要解封的IP地址") @PathVariable String ip) {
        loginAnomalyAlertService.unbanIp(ip);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/login-trend")
    @Operation(summary = "获取登录趋势", description = "返回最近7天每日登录成功与失败次数")
    public ResponseEntity<ApiResponse<List<LoginTrendPoint>>> getLoginTrend() {
        List<LoginTrendPoint> trend = securityDashboardService.getLoginTrend();
        return ResponseEntity.ok(ApiResponse.success(trend));
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "获取当前用户会话列表", description = "返回当前登录用户的所有活跃会话")
    public ResponseEntity<ApiResponse<List<SessionInfo>>> getSessions(Authentication authentication) {
        String username = authentication.getName();
        List<SessionInfo> sessions = sessionService.getUserSessions(username, null);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @DeleteMapping("/sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "终止指定会话", description = "终止当前用户的指定会话")
    public ResponseEntity<ApiResponse<Void>> terminateSession(
            Authentication authentication,
            @Parameter(description = "会话ID") @PathVariable Long id) {
        sessionService.terminateSession(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "终止所有其他会话", description = "终止当前用户除当前会话外的所有会话")
    public ResponseEntity<ApiResponse<Void>> terminateAllOtherSessions(Authentication authentication) {
        sessionService.terminateAllOtherSessions(authentication.getName(), null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/weekly-report/send")
    @Operation(summary = "发送安全周报", description = "生成并发送安全周报邮件给所有管理员")
    public ResponseEntity<ApiResponse<Void>> sendWeeklyReport() {
        securityWeeklyReportService.generateAndSendWeeklyReport();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/weekly-report/preview")
    @Operation(summary = "预览安全周报", description = "生成安全周报HTML内容用于预览")
    public ResponseEntity<ApiResponse<String>> previewWeeklyReport() {
        String html = securityWeeklyReportService.generateReportHtml();
        return ResponseEntity.ok(ApiResponse.success(html));
    }
}
