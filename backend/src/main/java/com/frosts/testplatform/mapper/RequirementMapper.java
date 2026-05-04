package com.frosts.testplatform.mapper;

import com.frosts.testplatform.dto.requirement.RequirementResponse;
import com.frosts.testplatform.entity.Requirement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequirementMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "parent.title", target = "parentTitle")
    RequirementResponse toResponse(Requirement requirement);
}
