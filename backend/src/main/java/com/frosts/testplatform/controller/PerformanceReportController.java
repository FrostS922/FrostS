package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceCompareResponse;
import com.frosts.testplatform.dto.performancereport.DiagnosisResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceLogResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceOverviewResponse;
import com.frosts.testplatform.dto.performancereport.PerformancePercentileResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceReportRequest;
import com.frosts.testplatform.dto.performancereport.PerformanceTrendResponse;
import com.frosts.testplatform.service.PerformanceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/performance-report")
@RequiredArgsConstructor
@Tag(name = "性能报告", description = "性能数据上报与分析")
public class PerformanceReportController {

    private final PerformanceReportService performanceReportService;

    @PostMapping
    @Operation(summary = "上报性能数据")
    public ResponseEntity<ApiResponse<Void>> reportPerformance(@Valid @RequestBody PerformanceReportRequest request) {
        performanceReportService.reportPerformance(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "分页查询性能日志")
    public ResponseEntity<ApiResponse<List<PerformanceLogResponse>>> getPerformanceLogs(
            @RequestParam(required = false) @Parameter(description = "指标名称") String metricName,
            @RequestParam(required = false) @Parameter(description = "开始时间") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @Parameter(description = "结束时间") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页数量") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created_at"));
        Page<PerformanceLogResponse> result = performanceReportService.getPerformanceLogs(metricName, startTime, endTime, pageable);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), result.getTotalElements()));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取性能概览")
    public ResponseEntity<ApiResponse<PerformanceOverviewResponse>> getPerformanceOverview() {
        return ResponseEntity.ok(ApiResponse.success(performanceReportService.getPerformanceOverview()));
    }

    @GetMapping("/trend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取性能趋势")
    public ResponseEntity<ApiResponse<PerformanceTrendResponse>> getPerformanceTrend(
            @RequestParam(required = false) @Parameter(description = "天数") Integer days) {
        return ResponseEntity.ok(ApiResponse.success(performanceReportService.getPerformanceTrend(days)));
    }

    @GetMapping("/percentiles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取性能百分位数")
    public ResponseEntity<ApiResponse<PerformancePercentileResponse>> getPerformancePercentiles(
            @RequestParam(required = false) @Parameter(description = "天数") Integer days) {
        return ResponseEntity.ok(ApiResponse.success(performanceReportService.getPerformancePercentiles(days)));
    }

    @GetMapping("/compare")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "性能对比分析")
    public ResponseEntity<ApiResponse<PerformanceCompareResponse>> comparePerformance(
            @RequestParam @Parameter(description = "基准开始时间") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseStart,
            @RequestParam @Parameter(description = "基准结束时间") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseEnd,
            @RequestParam @Parameter(description = "对比开始时间") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime compareStart,
            @RequestParam @Parameter(description = "对比结束时间") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime compareEnd) {
        return ResponseEntity.ok(ApiResponse.success(
                performanceReportService.comparePerformance(baseStart, baseEnd, compareStart, compareEnd)));
    }

    @GetMapping("/diagnose")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "性能诊断")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> diagnose(
            @RequestParam @Parameter(description = "开始时间") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @Parameter(description = "结束时间") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(ApiResponse.success(performanceReportService.diagnose(startTime, endTime)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除性能日志")
    public ResponseEntity<ApiResponse<Void>> deletePerformanceLog(@PathVariable @Parameter(description = "日志ID") Long id) {
        performanceReportService.deletePerformanceLog(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
