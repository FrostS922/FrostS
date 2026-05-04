package com.frosts.testplatform.mapper;

import com.frosts.testplatform.dto.testplan.TestPlanResponse;
import com.frosts.testplatform.entity.TestPlan;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TestPlanMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    TestPlanResponse toResponse(TestPlan testPlan);
}
