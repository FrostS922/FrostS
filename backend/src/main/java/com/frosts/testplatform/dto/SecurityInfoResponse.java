package com.frosts.testplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityInfoResponse {

    private LocalDateTime passwordChangedAt;
    private Boolean accountNonLocked;
    private String lockReason;
    private Integer loginFailCount;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private Integer loginCount;
    private List<LoginHistoryItem> recentLogins;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginHistoryItem {
        private Long id;
        private LocalDateTime loginAt;
        private String loginIp;
        private String userAgent;
        private Boolean success;
        private String failReason;
    }
}
