package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.report.ReportExportDataResponse;
import com.frosts.testplatform.service.ErrorReportService;
import com.frosts.testplatform.service.PerformanceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "报告导出", description = "性能与错误报告数据导出")
public class ReportExportController {

    private final PerformanceReportService performanceReportService;
    private final ErrorReportService errorReportService;

    @GetMapping("/export-data")
    @Operation(summary = "获取报告导出数据", description = "根据时间范围获取性能概览、趋势、百分位、诊断和错误统计的完整报告数据")
    public ResponseEntity<ApiResponse<ReportExportDataResponse>> getExportData(
            @Parameter(description = "开始时间（ISO格式）") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间（ISO格式）") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        ReportExportDataResponse.ErrorStats errorStats = errorReportService.getErrorStats(startTime, endTime);

        ReportExportDataResponse response = ReportExportDataResponse.builder()
                .overview(performanceReportService.getPerformanceOverview())
                .trend(performanceReportService.getPerformanceTrend(null))
                .percentiles(performanceReportService.getPerformancePercentiles(null))
                .diagnosis(performanceReportService.diagnose(startTime, endTime))
                .errorStats(errorStats)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
