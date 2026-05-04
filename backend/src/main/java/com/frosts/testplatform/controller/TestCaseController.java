package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.testcase.TestCaseRequest;
import com.frosts.testplatform.dto.testcase.TestCaseResponse;
import com.frosts.testplatform.service.TestCaseService;
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
@RequestMapping("/projects/{projectId}/testcases")
@RequiredArgsConstructor
@Tag(name = "测试用例", description = "测试用例CRUD")
public class TestCaseController {

    private final TestCaseService testCaseService;

    @GetMapping
    @Operation(summary = "分页查询测试用例列表")
    public ResponseEntity<ApiResponse<List<TestCaseResponse>>> getTestCases(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") int size) {
        Page<TestCaseResponse> testCases = testCaseService.getTestCaseResponsesByProject(projectId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(testCases.getContent(), testCases.getTotalElements()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取测试用例详情")
    public ResponseEntity<ApiResponse<TestCaseResponse>> getTestCase(@PathVariable @Parameter(description = "用例ID") Long id) {
        return ResponseEntity.ok(ApiResponse.success(testCaseService.getTestCaseResponseById(id)));
    }

    @PostMapping
    @Operation(summary = "创建测试用例")
    public ResponseEntity<ApiResponse<TestCaseResponse>> createTestCase(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @Valid @RequestBody TestCaseRequest request) {
        request.setProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(testCaseService.createTestCase(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新测试用例")
    public ResponseEntity<ApiResponse<TestCaseResponse>> updateTestCase(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @PathVariable @Parameter(description = "用例ID") Long id,
            @Valid @RequestBody TestCaseRequest request) {
        request.setProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(testCaseService.updateTestCase(id, request)));
    }

    @GetMapping("/by-requirement/{requirementId}")
    @Operation(summary = "按需求查询测试用例")
    public ResponseEntity<ApiResponse<List<TestCaseResponse>>> getTestCasesByRequirement(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @PathVariable @Parameter(description = "需求ID") Long requirementId) {
        return ResponseEntity.ok(ApiResponse.success(testCaseService.getTestCaseResponsesByRequirement(projectId, requirementId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除测试用例")
    public ResponseEntity<ApiResponse<Void>> deleteTestCase(@PathVariable @Parameter(description = "用例ID") Long id) {
        testCaseService.deleteTestCase(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
