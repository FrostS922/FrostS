package com.frosts.testplatform.dto.testplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRemoveTestCasesRequest {

    @NotEmpty(message = "请选择要移除的用例")
    @Size(max = 100, message = "单次批量操作不能超过100条")
    private List<Long> planCaseIds;
}
