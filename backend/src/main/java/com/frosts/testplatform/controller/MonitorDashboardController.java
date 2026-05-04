package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.monitor.RealtimeSummaryResponse;
import com.frosts.testplatform.entity.Notification;
import com.frosts.testplatform.service.MonitorDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "监控仪表盘", description = "系统实时监控数据概览与告警")
public class MonitorDashboardController {

    private final MonitorDashboardService monitorDashboardService;

    @GetMapping("/realtime-summary")
    @Operation(summary = "获取实时监控概览", description = "返回性能、错误、安全三个维度的实时汇总数据")
    public ResponseEntity<ApiResponse<RealtimeSummaryResponse>> getRealtimeSummary() {
        RealtimeSummaryResponse response = monitorDashboardService.getRealtimeSummary();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/alerts/recent")
    @Operation(summary = "获取最近告警", description = "返回最近10条性能监控、错误、安全类告警通知")
    public ResponseEntity<ApiResponse<List<Notification>>> getRecentAlerts() {
        List<Notification> alerts = monitorDashboardService.getRecentAlerts();
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }
}
