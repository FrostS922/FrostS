package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.entity.Requirement;
import com.frosts.testplatform.service.RequirementService;
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
public class RequirementController {

    private final RequirementService requirementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Requirement>>> getRequirements(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Requirement> requirements = requirementService.getRequirementsByProject(projectId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(requirements.getContent(), requirements.getTotalElements()));
    }

    @GetMapping("/root")
    public ResponseEntity<ApiResponse<List<Requirement>>> getRootRequirements(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(requirementService.getRootRequirements(projectId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Requirement>> getRequirement(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(requirementService.getRequirementById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Requirement>> createRequirement(@PathVariable Long projectId, @RequestBody Requirement requirement) {
        return ResponseEntity.ok(ApiResponse.success(requirementService.createRequirement(requirement)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Requirement>> updateRequirement(@PathVariable Long id, @RequestBody Requirement requirement) {
        return ResponseEntity.ok(ApiResponse.success(requirementService.updateRequirement(id, requirement)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRequirement(@PathVariable Long id) {
        requirementService.deleteRequirement(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
