package com.frosts.testplatform.dto.errorreport;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteErrorLogsRequest {

    @NotEmpty(message = "ID列表不能为空")
    @Size(max = 100, message = "单次最多删除100条")
    private List<Long> ids;
}
