package com.frosts.testplatform.dto.errorreport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorOverviewResponse {

    private long totalErrors;
    private long todayErrors;
    private List<RecentError> recentErrors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentError {
        private Long id;
        private String errorMessage;
        private Integer httpStatus;
        private String pageUrl;
        private String createdAt;
    }
}
