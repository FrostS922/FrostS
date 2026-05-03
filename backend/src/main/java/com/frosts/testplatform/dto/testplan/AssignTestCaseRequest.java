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
public class AssignTestCaseRequest {

    @NotBlank(message = "执行人不能为空")
    private String assignedTo;
}
