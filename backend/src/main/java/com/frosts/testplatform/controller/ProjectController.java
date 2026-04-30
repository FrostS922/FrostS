package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.entity.Project;
import com.frosts.testplatform.service.ProjectService;
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
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Project>>> getProjects(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Project> projects = projectService.getAllProjects(search,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(projects.getContent(), projects.getTotalElements()));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<Project>>> getProjectList() {
        return ResponseEntity.ok(ApiResponse.success(projectService.getAllProjectsList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.getProjectById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Project>> createProject(@RequestBody Project project) {
        return ResponseEntity.ok(ApiResponse.success(projectService.createProject(project)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Project>> updateProject(@PathVariable Long id, @RequestBody Project project) {
        return ResponseEntity.ok(ApiResponse.success(projectService.updateProject(id, project)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/members/{username}")
    public ResponseEntity<ApiResponse<Project>> addMember(@PathVariable Long id, @PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(projectService.addMember(id, username)));
    }

    @DeleteMapping("/{id}/members/{username}")
    public ResponseEntity<ApiResponse<Project>> removeMember(@PathVariable Long id, @PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(projectService.removeMember(id, username)));
    }
}
