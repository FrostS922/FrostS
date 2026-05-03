package com.frosts.testplatform.dto.testplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteTestCaseRequest {

    @NotBlank(message = "执行状态不能为空")
    private String status;

    private String actualResult;

    @NotBlank(message = "执行人不能为空")
    private String executedBy;

    private String defectId;

    private String defectLink;

    private String evidence;

    private String blockReason;
}
