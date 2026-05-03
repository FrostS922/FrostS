package com.frosts.testplatform.dto.apm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceListResponse {
    private List<TraceItem> traces;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraceItem {
        private String traceId;
        private String serviceName;
        private String endpoint;
        private long duration;
        private int spanCount;
        private String status;
        private String startTime;
        private boolean isError;
    }
}
