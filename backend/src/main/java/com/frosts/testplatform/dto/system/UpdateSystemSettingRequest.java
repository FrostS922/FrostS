package com.frosts.testplatform.dto.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSystemSettingRequest(
        @NotBlank @Size(max = 100) String settingKey,
        @Size(max = 2000) String settingValue
) {
}
