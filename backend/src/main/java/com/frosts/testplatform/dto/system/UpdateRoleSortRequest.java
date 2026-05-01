package com.frosts.testplatform.dto.system;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateRoleSortRequest(
        @NotEmpty(message = "角色ID列表不能为空")
        List<Long> roleIds
) {
}
