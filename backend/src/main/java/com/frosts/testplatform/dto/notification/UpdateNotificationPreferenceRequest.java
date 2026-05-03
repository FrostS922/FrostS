package com.frosts.testplatform.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationPreferenceRequest {

    private Map<String, Boolean> typeSettings;
    private Map<String, Boolean> categorySettings;
    private Map<String, Boolean> receiveChannels;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
}
