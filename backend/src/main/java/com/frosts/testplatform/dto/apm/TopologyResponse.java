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
public class TopologyResponse {
    private List<TopologyNode> nodes;
    private List<TopologyEdge> edges;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopologyNode {
        private String name;
        private String type;
        private String healthStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopologyEdge {
        private String source;
        private String target;
        private long callCount;
        private double errorRate;
    }
}
