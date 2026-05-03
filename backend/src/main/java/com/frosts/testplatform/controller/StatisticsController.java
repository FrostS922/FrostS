package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.ProjectStatisticsDTO;
import com.frosts.testplatform.service.StatisticsService;
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
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProjectStatisticsDTO>> getProjectStatistics(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getProjectStatistics(projectId)));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(@PathVariable Long projectId) {
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
