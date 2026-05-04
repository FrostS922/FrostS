package com.frosts.testplatform.dto.report;

import com.frosts.testplatform.dto.performancereport.DiagnosisResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceCompareResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceOverviewResponse;
import com.frosts.testplatform.dto.performancereport.PerformancePercentileResponse;
import com.frosts.testplatform.dto.performancereport.PerformanceTrendResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "报告导出数据响应")
public class ReportExportDataResponse {
    @Schema(description = "性能概览")
    private PerformanceOverviewResponse overview;
    @Schema(description = "性能趋势")
    private PerformanceTrendResponse trend;
    @Schema(description = "性能百分位")
    private PerformancePercentileResponse percentiles;
    @Schema(description = "诊断结果")
    private DiagnosisResponse diagnosis;
    @Schema(description = "错误统计")
    private ErrorStats errorStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "错误统计")
    public static class ErrorStats {
        @Schema(description = "错误总数", example = "45")
        private long totalErrors;
        @Schema(description = "今日错误数", example = "3")
        private long todayErrors;
        @Schema(description = "Top错误分类")
        private java.util.List<ErrorCategoryCount> topCategories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "错误分类计数")
    public static class ErrorCategoryCount {
        @Schema(description = "错误分类", example = "NETWORK")
        private String category;
        @Schema(description = "错误数量", example = "12")
        private long count;
    }
}
