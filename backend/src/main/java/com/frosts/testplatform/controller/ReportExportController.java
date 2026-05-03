package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.report.ReportExportDataResponse;
import com.frosts.testplatform.repository.ErrorLogRepository;
import com.frosts.testplatform.service.PerformanceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportExportController {

    private final PerformanceReportService performanceReportService;
    private final ErrorLogRepository errorLogRepository;

    @GetMapping("/export-data")
    public ResponseEntity<ApiResponse<ReportExportDataResponse>> getExportData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        ReportExportDataResponse.ErrorStats errorStats = buildErrorStats(startTime, endTime);

        ReportExportDataResponse response = ReportExportDataResponse.builder()
                .overview(performanceReportService.getPerformanceOverview())
                .trend(performanceReportService.getPerformanceTrend(null))
                .percentiles(performanceReportService.getPerformancePercentiles(null))
                .diagnosis(performanceReportService.diagnose(startTime, endTime))
                .errorStats(errorStats)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private ReportExportDataResponse.ErrorStats buildErrorStats(LocalDateTime startTime, LocalDateTime endTime) {
        long totalErrors = errorLogRepository.countByIsDeletedFalse();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayErrors = errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(todayStart);

        List<Object[]> categoryCounts = errorLogRepository.countByCategoryInRange(startTime, endTime);
        List<ReportExportDataResponse.ErrorCategoryCount> topCategories = categoryCounts.stream()
                .limit(3)
                .map(row -> ReportExportDataResponse.ErrorCategoryCount.builder()
                        .category((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();

        return ReportExportDataResponse.ErrorStats.builder()
                .totalErrors(totalErrors)
                .todayErrors(todayErrors)
                .topCategories(topCategories)
                .build();
    }
}
