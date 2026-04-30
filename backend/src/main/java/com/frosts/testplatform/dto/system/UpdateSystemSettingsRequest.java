package com.frosts.testplatform.dto.system;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateSystemSettingsRequest(
        @NotEmpty List<@Valid UpdateSystemSettingRequest> settings
) {
}
