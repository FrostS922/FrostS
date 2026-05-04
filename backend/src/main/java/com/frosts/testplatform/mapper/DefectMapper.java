package com.frosts.testplatform.mapper;

import com.frosts.testplatform.dto.defect.DefectResponse;
import com.frosts.testplatform.entity.Defect;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DefectMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    @Mapping(source = "testCase.id", target = "testCaseId")
    @Mapping(source = "testCase.title", target = "testCaseTitle")
    DefectResponse toResponse(Defect defect);
}
