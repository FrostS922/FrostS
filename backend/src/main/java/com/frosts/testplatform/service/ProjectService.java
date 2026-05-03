package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.Project;
import com.frosts.testplatform.entity.User;
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

    public Project createProject(Project project) {
        if (project.getCode() != null && projectRepository.existsByCodeAndIsDeletedFalse(project.getCode())) {
            throw new RuntimeException("项目编码已存在: " + project.getCode());
        }
        if (project.getProgress() == null) {
            project.setProgress(new java.math.BigDecimal("0.00"));
        }
        if (project.getHealth() == null) {
            project.setHealth("NORMAL");
        }
        return projectRepository.save(project);
    }

    public Project updateProject(Long id, Project projectDetails) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("项目不存在: " + id));

        project.setName(projectDetails.getName());
        project.setDescription(projectDetails.getDescription());
        project.setManager(projectDetails.getManager());
        project.setStartDate(projectDetails.getStartDate());
        project.setEndDate(projectDetails.getEndDate());
        project.setActualEndDate(projectDetails.getActualEndDate());
        project.setStatus(projectDetails.getStatus());
        project.setCategory(projectDetails.getCategory());
        project.setProgress(projectDetails.getProgress());
        project.setEstimatedHours(projectDetails.getEstimatedHours());
        project.setActualHours(projectDetails.getActualHours());
        project.setHealth(projectDetails.getHealth());

        return projectRepository.save(project);
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
}
