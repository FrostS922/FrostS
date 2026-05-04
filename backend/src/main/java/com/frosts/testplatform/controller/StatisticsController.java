package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.ProjectStatisticsDTO;
import com.frosts.testplatform.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/projects/{projectId}/statistics")
@RequiredArgsConstructor
@Tag(name = "项目统计", description = "项目数据统计概览")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    @Operation(summary = "获取项目统计数据")
    public ResponseEntity<ApiResponse<ProjectStatisticsDTO>> getProjectStatistics(@PathVariable @Parameter(description = "项目ID") Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getProjectStatistics(projectId)));
    }

    @GetMapping("/overview")
    @Operation(summary = "获取项目统计概览")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(@PathVariable @Parameter(description = "项目ID") Long projectId) {
        ProjectStatisticsDTO stats = statisticsService.getProjectStatistics(projectId);
        Map<String, Object> overview = Map.of(
                "totalRequirements", stats.getTotalRequirements(),
                "totalTestCases", stats.getTotalTestCases(),
                "totalTestPlans", stats.getTotalTestPlans(),
                "totalDefects", stats.getTotalDefects(),
                "automationRate", stats.getAutomationRate(),
                "averagePassRate", stats.getAveragePassRate(),
                "defectDensity", stats.getDefectDensity(),
                "reopenedDefects", stats.getReopenedDefects()
        );
        return ResponseEntity.ok(ApiResponse.success(overview));
    }
}
