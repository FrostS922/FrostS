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
public class PerformanceReportController {

    private final PerformanceReportService performanceReportService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> reportPerformance(@Valid @RequestBody PerformanceReportRequest request) {
        performanceReportService.reportPerformance(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PerformanceLogResponse>>> getPerformanceLogs(
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created_at"));
        Page<PerformanceLogResponse> result = performanceReportService.getPerformanceLogs(metricName, startTime, endTime, pageable);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), result.getTotalElements()));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PerformanceOverviewResponse>> getPerformanceOverview() {
        return ResponseEntity.ok(ApiResponse.success(performanceReportService.getPerformanceOverview()));
    }

    @GetMapping("/trend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PerformanceTrendResponse>> getPerformanceTrend(
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(ApiResponse.success(performanceReportService.getPerformanceTrend(days)));
    }

    @GetMapping("/percentiles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PerformancePercentileResponse>> getPerformancePercentiles(
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(ApiResponse.success(performanceReportService.getPerformancePercentiles(days)));
    }

    @GetMapping("/compare")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PerformanceCompareResponse>> comparePerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime baseEnd,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime compareStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime compareEnd) {
        return ResponseEntity.ok(ApiResponse.success(
                performanceReportService.comparePerformance(baseStart, baseEnd, compareStart, compareEnd)));
    }

    @GetMapping("/diagnose")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DiagnosisResponse>> diagnose(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ResponseEntity.ok(ApiResponse.success(performanceReportService.diagnose(startTime, endTime)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePerformanceLog(@PathVariable Long id) {
        performanceReportService.deletePerformanceLog(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
