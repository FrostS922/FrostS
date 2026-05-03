package com.frosts.testplatform.dto.report;

import com.frosts.testplatform.dto.performancereport.DiagnosisResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceCompareResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceOverviewResponse;
import com.frosts.testplatform.dto.performancereport.PerformancePercentileResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceTrendResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExportDataResponse {
    private PerformanceOverviewResponse overview;
    private PerformanceTrendResponse trend;
    private PerformancePercentileResponse percentiles;
    private DiagnosisResponse diagnosis;
    private ErrorStats errorStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorStats {
        private long totalErrors;
        private long todayErrors;
        private java.util.List<ErrorCategoryCount> topCategories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorCategoryCount {
        private String category;
        private long count;
    }
}
