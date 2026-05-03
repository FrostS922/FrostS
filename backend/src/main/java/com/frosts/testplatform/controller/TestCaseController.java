package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.entity.TestCase;
import com.frosts.testplatform.service.TestCaseService;
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
public class TestCaseController {

    private final TestCaseService testCaseService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TestCase>>> getTestCases(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TestCase> testCases = testCaseService.getTestCasesByProject(projectId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(testCases.getContent(), testCases.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestCase>> getTestCase(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(testCaseService.getTestCaseById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TestCase>> createTestCase(@PathVariable Long projectId, @RequestBody TestCase testCase) {
        return ResponseEntity.ok(ApiResponse.success(testCaseService.createTestCase(testCase)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestCase>> updateTestCase(@PathVariable Long id, @RequestBody TestCase testCase) {
        return ResponseEntity.ok(ApiResponse.success(testCaseService.updateTestCase(id, testCase)));
    }

    @GetMapping("/by-requirement/{requirementId}")
    public ResponseEntity<ApiResponse<List<TestCase>>> getTestCasesByRequirement(
            @PathVariable Long projectId,
            @PathVariable Long requirementId) {
        List<TestCase> testCases = testCaseService.getTestCasesByRequirement(projectId, requirementId);
        return ResponseEntity.ok(ApiResponse.success(testCases));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTestCase(@PathVariable Long id) {
        testCaseService.deleteTestCase(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
