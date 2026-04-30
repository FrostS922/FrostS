package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.entity.TestPlan;
import com.frosts.testplatform.entity.TestPlanCase;
import com.frosts.testplatform.service.TestPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/testplans")
@RequiredArgsConstructor
public class TestPlanController {

    private final TestPlanService testPlanService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TestPlan>>> getTestPlans(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TestPlan> testPlans = testPlanService.getTestPlansByProject(projectId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(testPlans.getContent(), testPlans.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestPlan>> getTestPlan(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.getTestPlanById(id)));
    }

    @GetMapping("/{planId}/cases")
    public ResponseEntity<ApiResponse<List<TestPlanCase>>> getTestPlanCases(@PathVariable Long planId) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.getTestPlanCases(planId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TestPlan>> createTestPlan(@RequestBody TestPlan testPlan) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.createTestPlan(testPlan)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestPlan>> updateTestPlan(@PathVariable Long id, @RequestBody TestPlan testPlan) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.updateTestPlan(id, testPlan)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTestPlan(@PathVariable Long id) {
        testPlanService.deleteTestPlan(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{planId}/cases")
    public ResponseEntity<ApiResponse<TestPlanCase>> addTestCase(@PathVariable Long planId, @RequestBody TestPlanCase testPlanCase) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.addTestCaseToPlan(planId, testPlanCase)));
    }

    @PostMapping("/cases/{caseId}/execute")
    public ResponseEntity<ApiResponse<TestPlanCase>> executeTestCase(
            @PathVariable Long caseId,
            @RequestParam String status,
            @RequestParam String actualResult,
            @RequestParam String executedBy) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.executeTestCase(caseId, status, actualResult, executedBy)));
    }
}
