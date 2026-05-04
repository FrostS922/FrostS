package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.project.ProjectRequest;
import com.frosts.testplatform.dto.project.ProjectResponse;
import com.frosts.testplatform.entity.Project;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.mapper.ProjectMapper;
import com.frosts.testplatform.repository.ProjectRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    @Transactional(readOnly = true)
    public Page<Project> getAllProjects(String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return projectRepository.findByNameContainingOrCodeContainingAndIsDeletedFalse(search, search, pageable);
        }
        return projectRepository.findByIsDeletedFalse(pageable);
    }

    @Transactional(readOnly = true)
    public Project getProjectById(Long id) {
        return projectRepository.findByIdWithMembers(id);
    }

    @Transactional(readOnly = true)
    public List<Project> getAllProjectsList() {
        return projectRepository.findByIsDeletedFalse();
    }

    public ProjectResponse createProject(ProjectRequest request) {
        if (request.getCode() != null && projectRepository.existsByCodeAndIsDeletedFalse(request.getCode())) {
            throw new RuntimeException("项目编码已存在: " + request.getCode());
        }
        Project project = new Project();
        project.setName(request.getName());
        project.setCode(request.getCode());
        project.setDescription(request.getDescription());
        project.setManager(request.getManager());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setActualEndDate(request.getActualEndDate());
        project.setStatus(request.getStatus());
        project.setCategory(request.getCategory());
        project.setProgress(request.getProgress() != null ? request.getProgress() : new java.math.BigDecimal("0.00"));
        project.setEstimatedHours(request.getEstimatedHours());
        project.setActualHours(request.getActualHours());
        project.setHealth(request.getHealth() != null ? request.getHealth() : "NORMAL");
        if (project.getStatus() == null) project.setStatus("ACTIVE");
        return projectMapper.toResponse(projectRepository.save(project));
    }

    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = getProjectById(id);
        project.setName(request.getName());
        project.setCode(request.getCode());
        project.setDescription(request.getDescription());
        project.setManager(request.getManager());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setActualEndDate(request.getActualEndDate());
        project.setStatus(request.getStatus());
        project.setCategory(request.getCategory());
        project.setProgress(request.getProgress());
        project.setEstimatedHours(request.getEstimatedHours());
        project.setActualHours(request.getActualHours());
        project.setHealth(request.getHealth());
        return projectMapper.toResponse(projectRepository.save(project));
    }

    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("项目不存在: " + id));
        project.setIsDeleted(true);
        projectRepository.save(project);
    }

    public Project addMember(Long projectId, String username) {
        Project project = projectRepository.findByIdWithMembers(projectId);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
        project.getMembers().add(user);
        return projectRepository.save(project);
    }

    public Project removeMember(Long projectId, String username) {
        Project project = projectRepository.findByIdWithMembers(projectId);
        project.getMembers().removeIf(u -> u.getUsername().equals(username));
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> getProjectResponses(String search, Pageable pageable) {
        Page<Project> page;
        if (search != null && !search.isEmpty()) {
            page = projectRepository.findByNameContainingOrCodeContainingAndIsDeletedFalse(search, search, pageable);
        } else {
            page = projectRepository.findByIsDeletedFalse(pageable);
        }
        return page.map(projectMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectResponseById(Long id) {
        Project p = getProjectById(id);
        return projectMapper.toResponse(p);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjectResponsesList() {
        return projectRepository.findByIsDeletedFalse().stream().map(projectMapper::toResponse).toList();
    }
}
