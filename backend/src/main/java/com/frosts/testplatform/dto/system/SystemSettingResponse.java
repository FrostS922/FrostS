package com.frosts.testplatform.dto.system;

import java.time.LocalDateTime;
import java.util.List;

public record SystemSettingResponse(
        Long id,
        String settingKey,
        String settingValue,
        String defaultValue,
        String name,
        String category,
        String valueType,
        List<String> options,
        String description,
        Integer sortOrder,
        Boolean editable,
        LocalDateTime updatedAt
) {
}
