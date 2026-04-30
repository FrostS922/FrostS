package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.entity.Defect;
import com.frosts.testplatform.service.DefectService;
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
public class DefectController {

    private final DefectService defectService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Defect>>> getDefects(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Defect> defects = defectService.getDefectsByProject(projectId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(defects.getContent(), defects.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Defect>> getDefect(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(defectService.getDefectById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Defect>> createDefect(@RequestBody Defect defect) {
        return ResponseEntity.ok(ApiResponse.success(defectService.createDefect(defect)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Defect>> updateDefect(@PathVariable Long id, @RequestBody Defect defect) {
        return ResponseEntity.ok(ApiResponse.success(defectService.updateDefect(id, defect)));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<Defect>> resolveDefect(
            @PathVariable Long id,
            @RequestParam String resolution,
            @RequestParam String resolvedBy) {
        return ResponseEntity.ok(ApiResponse.success(defectService.resolveDefect(id, resolution, resolvedBy)));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<Defect>> closeDefect(
            @PathVariable Long id,
            @RequestParam String closedBy) {
        return ResponseEntity.ok(ApiResponse.success(defectService.closeDefect(id, closedBy)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDefect(@PathVariable Long id) {
        defectService.deleteDefect(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(defectService.getDefectStatistics(projectId)));
    }
}
