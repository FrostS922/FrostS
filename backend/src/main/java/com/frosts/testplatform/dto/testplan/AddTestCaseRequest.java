package com.frosts.testplatform.dto.testplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddTestCaseRequest {

    @NotNull(message = "测试用例ID不能为空")
    private Long testCaseId;

    private String priority;

    private String assignedTo;
}
