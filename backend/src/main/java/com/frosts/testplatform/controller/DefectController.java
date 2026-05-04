package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.defect.DefectRequest;
import com.frosts.testplatform.dto.defect.DefectResponse;
import com.frosts.testplatform.service.DefectService;
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
import java.util.Map;

@RestController
@RequestMapping("/projects/{projectId}/defects")
@RequiredArgsConstructor
@Tag(name = "缺陷管理", description = "缺陷CRUD与状态流转")
public class DefectController {

    private final DefectService defectService;

    @GetMapping
    @Operation(summary = "分页查询缺陷列表")
    public ResponseEntity<ApiResponse<List<DefectResponse>>> getDefects(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") int size) {
        Page<DefectResponse> defects = defectService.getDefectResponsesByProject(projectId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(defects.getContent(), defects.getTotalElements()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取缺陷详情")
    public ResponseEntity<ApiResponse<DefectResponse>> getDefect(@PathVariable @Parameter(description = "缺陷ID") Long id) {
        return ResponseEntity.ok(ApiResponse.success(defectService.getDefectResponseById(id)));
    }

    @PostMapping
    @Operation(summary = "创建缺陷")
    public ResponseEntity<ApiResponse<DefectResponse>> createDefect(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @Valid @RequestBody DefectRequest request) {
        request.setProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(defectService.createDefect(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新缺陷")
    public ResponseEntity<ApiResponse<DefectResponse>> updateDefect(
            @PathVariable @Parameter(description = "项目ID") Long projectId,
            @PathVariable @Parameter(description = "缺陷ID") Long id,
            @Valid @RequestBody DefectRequest request) {
        request.setProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(defectService.updateDefect(id, request)));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "解决缺陷")
    public ResponseEntity<ApiResponse<DefectResponse>> resolveDefect(
            @PathVariable @Parameter(description = "缺陷ID") Long id,
            @RequestParam @Parameter(description = "解决方案") String resolution,
            @RequestParam @Parameter(description = "解决人") String resolvedBy) {
        return ResponseEntity.ok(ApiResponse.success(defectService.resolveDefect(id, resolution, resolvedBy)));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭缺陷")
    public ResponseEntity<ApiResponse<DefectResponse>> closeDefect(
            @PathVariable @Parameter(description = "缺陷ID") Long id,
            @RequestParam @Parameter(description = "关闭人") String closedBy) {
        return ResponseEntity.ok(ApiResponse.success(defectService.closeDefect(id, closedBy)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除缺陷")
    public ResponseEntity<ApiResponse<Void>> deleteDefect(@PathVariable @Parameter(description = "缺陷ID") Long id) {
        defectService.deleteDefect(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取缺陷统计")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(@PathVariable @Parameter(description = "项目ID") Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(defectService.getDefectStatistics(projectId)));
    }
}
