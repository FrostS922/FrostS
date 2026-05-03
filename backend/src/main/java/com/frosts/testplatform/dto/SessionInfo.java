package com.frosts.testplatform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SessionInfo {
    private Long id;
    private String deviceInfo;
    private String clientIp;
    private LocalDateTime createdAt;
    private LocalDateTime lastRefreshedAt;
    private Boolean current;
}
