package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.testplan.*;
import com.frosts.testplatform.service.TestPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "测试计划", description = "测试计划与用例执行管理")
public class TestPlanController {

    private final TestPlanService testPlanService;

    @GetMapping
    @Operation(summary = "分页查询测试计划列表")
    public ResponseEntity<ApiResponse<List<TestPlanResponse>>> getTestPlans(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") int size) {
        Page<TestPlanResponse> testPlans = testPlanService.getTestPlanResponsesByProject(projectId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(testPlans.getContent(), testPlans.getTotalElements()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取测试计划详情")
    public ResponseEntity<ApiResponse<TestPlanResponse>> getTestPlan(@PathVariable @Parameter(description = "测试计划ID") Long id) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.getTestPlanResponseById(id)));
    }

    @GetMapping("/{planId}/cases")
    @Operation(summary = "获取测试计划下的用例列表")
    public ResponseEntity<ApiResponse<List<TestPlanCaseResponse>>> getTestPlanCases(@PathVariable @Parameter(description = "测试计划ID") Long planId) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.getTestPlanCases(planId)));
    }

    @PostMapping
    @Operation(summary = "创建测试计划")
    public ResponseEntity<ApiResponse<TestPlanResponse>> createTestPlan(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @Valid @RequestBody TestPlanRequest request) {
        request.setProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(testPlanService.createTestPlan(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新测试计划")
    public ResponseEntity<ApiResponse<TestPlanResponse>> updateTestPlan(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @PathVariable @Parameter(description = "测试计划ID") Long id,
            @Valid @RequestBody TestPlanRequest request) {
        request.setProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(testPlanService.updateTestPlan(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除测试计划")
    public ResponseEntity<ApiResponse<Void>> deleteTestPlan(@PathVariable @Parameter(description = "测试计划ID") Long id) {
        testPlanService.deleteTestPlan(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{planId}/cases")
    @Operation(summary = "添加测试用例到计划")
    public ResponseEntity<ApiResponse<TestPlanCaseResponse>> addTestCase(
            @PathVariable @Parameter(description = "测试计划ID") Long planId,
            @Valid @RequestBody AddTestCaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.addTestCaseToPlan(planId, request)));
    }

    @PostMapping("/cases/{caseId}/execute")
    @Operation(summary = "执行测试用例")
    public ResponseEntity<ApiResponse<TestPlanCaseResponse>> executeTestCase(
            @PathVariable @Parameter(description = "用例ID") Long caseId,
            @Valid @RequestBody ExecuteTestCaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.executeTestCase(caseId, request)));
    }

    @PostMapping("/{planId}/cases/batch")
    @Operation(summary = "批量添加测试用例")
    public ResponseEntity<ApiResponse<List<TestPlanCaseResponse>>> batchAddTestCases(
            @PathVariable @Parameter(description = "测试计划ID") Long planId,
            @Valid @RequestBody BatchAddTestCasesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.batchAddTestCases(planId, request)));
    }

    @DeleteMapping("/{planId}/cases/batch")
    @Operation(summary = "批量移除测试用例")
    public ResponseEntity<ApiResponse<Void>> batchRemoveTestCases(
            @PathVariable @Parameter(description = "测试计划ID") Long planId,
            @Valid @RequestBody BatchRemoveTestCasesRequest request) {
        testPlanService.batchRemoveTestCases(planId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/cases/batch-execute")
    @Operation(summary = "批量执行测试用例")
    public ResponseEntity<ApiResponse<List<TestPlanCaseResponse>>> batchExecuteTestCases(
            @Valid @RequestBody BatchExecuteTestCasesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.batchExecuteTestCases(request)));
    }

    @PutMapping("/cases/{caseId}/assign")
    @Operation(summary = "分配测试用例")
    public ResponseEntity<ApiResponse<TestPlanCaseResponse>> assignTestCase(
            @PathVariable @Parameter(description = "用例ID") Long caseId,
            @Valid @RequestBody AssignTestCaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.assignTestCase(caseId, request)));
    }

    @PostMapping("/{planId}/cases/batch-assign")
    @Operation(summary = "批量分配测试用例")
    public ResponseEntity<ApiResponse<List<TestPlanCaseResponse>>> batchAssignTestCases(
            @PathVariable @Parameter(description = "测试计划ID") Long planId,
            @Valid @RequestBody BatchAssignTestCasesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(testPlanService.batchAssignTestCases(planId, request)));
    }
}
