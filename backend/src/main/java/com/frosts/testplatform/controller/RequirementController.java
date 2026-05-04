package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.RequirementCoverageDTO;
import com.frosts.testplatform.dto.requirement.RequirementRequest;
import com.frosts.testplatform.dto.requirement.RequirementResponse;
import com.frosts.testplatform.service.RequirementService;
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
@RequestMapping("/projects/{projectId}/requirements")
@RequiredArgsConstructor
@Tag(name = "需求管理", description = "项目需求CRUD与覆盖率")
public class RequirementController {

    private final RequirementService requirementService;

    @GetMapping
    @Operation(summary = "分页查询需求列表")
    public ResponseEntity<ApiResponse<List<RequirementResponse>>> getRequirements(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") int size) {
        Page<RequirementResponse> requirements = requirementService.getRequirementResponsesByProject(projectId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(requirements.getContent(), requirements.getTotalElements()));
    }

    @GetMapping("/root")
    @Operation(summary = "获取根需求列表")
    public ResponseEntity<ApiResponse<List<RequirementResponse>>> getRootRequirements(@PathVariable @Parameter(description = "项目ID") Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(requirementService.getRootRequirementResponses(projectId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取需求详情")
    public ResponseEntity<ApiResponse<RequirementResponse>> getRequirement(@PathVariable @Parameter(description = "需求ID") Long id) {
        return ResponseEntity.ok(ApiResponse.success(requirementService.getRequirementResponseById(id)));
    }

    @PostMapping
    @Operation(summary = "创建需求")
    public ResponseEntity<ApiResponse<RequirementResponse>> createRequirement(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @Valid @RequestBody RequirementRequest request) {
        request.setProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(requirementService.createRequirement(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新需求")
    public ResponseEntity<ApiResponse<RequirementResponse>> updateRequirement(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @PathVariable @Parameter(description = "需求ID") Long id,
            @Valid @RequestBody RequirementRequest request) {
        request.setProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(requirementService.updateRequirement(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除需求")
    public ResponseEntity<ApiResponse<Void>> deleteRequirement(@PathVariable @Parameter(description = "需求ID") Long id) {
        requirementService.deleteRequirement(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{requirementId}/coverage")
    @Operation(summary = "获取需求覆盖率")
    public ResponseEntity<ApiResponse<RequirementCoverageDTO>> getRequirementCoverage(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @PathVariable @Parameter(description = "需求ID") Long requirementId) {
        return ResponseEntity.ok(ApiResponse.success(requirementService.getRequirementCoverage(projectId, requirementId)));
    }
}
