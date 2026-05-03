package com.frosts.testplatform.dto.apm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceDetailResponse {
    private String traceId;
    private List<SpanDetail> spans;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpanDetail {
        private String spanId;
        private String parentSpanId;
        private String operationName;
        private long startTime;
        private long duration;
        private String status;
        private String serviceCode;
        private Map<String, String> tags;
    }
}
