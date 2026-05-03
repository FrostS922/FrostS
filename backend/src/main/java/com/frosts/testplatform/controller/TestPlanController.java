package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.testplan.*;
import com.frosts.testplatform.entity.TestPlan;
import com.frosts.testplatform.service.TestPlanService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ApiResponse<List<TestPlanCaseResponse>>> getTestPlanCases(@PathVariable Long planId) {
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
    public ResponseEntity<ApiResponse<TestPlanCaseResponse>> addTestCase(
            @PathVariable Long planId,
            @Valid @RequestBody AddTestCaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.addTestCaseToPlan(planId, request)));
    }

    @PostMapping("/cases/{caseId}/execute")
    public ResponseEntity<ApiResponse<TestPlanCaseResponse>> executeTestCase(
            @PathVariable Long caseId,
            @Valid @RequestBody ExecuteTestCaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.executeTestCase(caseId, request)));
    }

    @PostMapping("/{planId}/cases/batch")
    public ResponseEntity<ApiResponse<List<TestPlanCaseResponse>>> batchAddTestCases(
            @PathVariable Long planId,
            @Valid @RequestBody BatchAddTestCasesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.batchAddTestCases(planId, request)));
    }

    @DeleteMapping("/{planId}/cases/batch")
    public ResponseEntity<ApiResponse<Void>> batchRemoveTestCases(
            @PathVariable Long planId,
            @Valid @RequestBody BatchRemoveTestCasesRequest request) {
        testPlanService.batchRemoveTestCases(planId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/cases/batch-execute")
    public ResponseEntity<ApiResponse<List<TestPlanCaseResponse>>> batchExecuteTestCases(
            @Valid @RequestBody BatchExecuteTestCasesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.batchExecuteTestCases(request)));
    }

    @PutMapping("/cases/{caseId}/assign")
    public ResponseEntity<ApiResponse<TestPlanCaseResponse>> assignTestCase(
            @PathVariable Long caseId,
            @Valid @RequestBody AssignTestCaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.assignTestCase(caseId, request)));
    }

    @PostMapping("/{planId}/cases/batch-assign")
    public ResponseEntity<ApiResponse<List<TestPlanCaseResponse>>> batchAssignTestCases(
            @PathVariable Long planId,
            @Valid @RequestBody BatchAssignTestCasesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.batchAssignTestCases(planId, request)));
    }
}
