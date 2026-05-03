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
public class BatchAddTestCasesRequest {

    @NotEmpty(message = "用例ID列表不能为空")
    @Size(max = 100, message = "单次批量操作不能超过100条")
    private List<Long> testCaseIds;
}
