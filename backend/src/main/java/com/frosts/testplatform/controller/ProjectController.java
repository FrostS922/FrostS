package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.project.ProjectRequest;
import com.frosts.testplatform.dto.project.ProjectResponse;
import com.frosts.testplatform.service.ProjectService;
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
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "项目管理", description = "项目CRUD与成员管理")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @Operation(summary = "分页查询项目列表")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjects(
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String search,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") int size) {
        Page<ProjectResponse> projects = projectService.getProjectResponses(search,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(projects.getContent(), projects.getTotalElements()));
    }

    @GetMapping("/list")
    @Operation(summary = "获取全部项目列表")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getProjectList() {
        return ResponseEntity.ok(ApiResponse.success(projectService.getAllProjectResponsesList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取项目详情")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(@PathVariable @Parameter(description = "项目ID") Long id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectResponseById(id)));
    }

    @PostMapping
    @Operation(summary = "创建项目")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success(projectService.createProject(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新项目")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(@PathVariable @Parameter(description = "项目ID") Long id, @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success(projectService.updateProject(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除项目")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable @Parameter(description = "项目ID") Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/members/{username}")
    @Operation(summary = "添加项目成员")
    public ResponseEntity<ApiResponse<ProjectResponse>> addMember(
            @PathVariable @Parameter(description = "项目ID") Long id,
            @PathVariable @Parameter(description = "用户名") String username) {
        projectService.addMember(id, username);
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectResponseById(id)));
    }

    @DeleteMapping("/{id}/members/{username}")
    @Operation(summary = "移除项目成员")
    public ResponseEntity<ApiResponse<ProjectResponse>> removeMember(
            @PathVariable @Parameter(description = "项目ID") Long id,
            @PathVariable @Parameter(description = "用户名") String username) {
        projectService.removeMember(id, username);
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectResponseById(id)));
    }
}
