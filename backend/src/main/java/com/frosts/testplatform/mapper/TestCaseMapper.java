package com.frosts.testplatform.mapper;

import com.frosts.testplatform.dto.testcase.TestCaseResponse;
import com.frosts.testplatform.entity.TestCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TestCaseMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    @Mapping(source = "requirement.id", target = "requirementId")
    @Mapping(source = "requirement.title", target = "requirementTitle")
    @Mapping(source = "module.id", target = "moduleId")
    @Mapping(source = "module.name", target = "moduleName")
    TestCaseResponse toResponse(TestCase testCase);
}
