package com.frosts.testplatform.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {

    private long total;
    private long systemCount;
    private long businessCount;
    private long reminderCount;
    private long todoCount;
}
