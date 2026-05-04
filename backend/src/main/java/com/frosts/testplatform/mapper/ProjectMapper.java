package com.frosts.testplatform.mapper;

import com.frosts.testplatform.dto.project.ProjectResponse;
import com.frosts.testplatform.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "memberUsernames", expression = "java(mapMemberUsernames(project))")
    ProjectResponse toResponse(Project project);

    default List<String> mapMemberUsernames(Project project) {
        if (project.getMembers() == null) return List.of();
        return project.getMembers().stream().map(u -> u.getUsername()).toList();
    }
}
