package com.frosts.testplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frosts.testplatform.dto.apm.ServiceMetricResponse;
import com.frosts.testplatform.dto.apm.TopologyResponse;
import com.frosts.testplatform.dto.apm.TraceDetailResponse;
import com.frosts.testplatform.dto.apm.TraceListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class SkyWalkingApiClient {

    @Value("${skywalking.oap.url:http://localhost:12800}")
    private String oapUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SkyWalkingApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public List<ServiceMetricResponse> getServiceMetrics(String duration) {
        try {
            String servicesQuery = "query queryServices($duration: Duration!) { services(duration: $duration) { id name } }";
            Map<String, Object> servicesVars = Map.of("duration", Map.of("start", getStartTime(duration), "end", getEndTime(), "step", duration));
            JsonNode servicesResult = executeGraphQL(servicesQuery, servicesVars);

            if (servicesResult == null || !servicesResult.has("data") || !servicesResult.get("data").has("services")) {
                return Collections.emptyList();
            }

            List<ServiceMetricResponse> metrics = new ArrayList<>();
            JsonNode services = servicesResult.get("data").get("services");

            for (JsonNode service : services) {
                String serviceId = service.get("id").asText();
                String serviceName = service.get("name").asText();

                try {
                    ServiceMetricResponse metric = queryServiceMetrics(serviceId, serviceName, duration);
                    metrics.add(metric);
                } catch (Exception e) {
                    log.warn("Failed to query metrics for service: {}, error: {}", serviceName, e.getMessage());
                    metrics.add(ServiceMetricResponse.builder()
                            .serviceName(serviceName)
                            .avgResponseTime(0)
                            .p99(0)
                            .throughput(0)
                            .errorRate(0)
                            .healthStatus("UNKNOWN")
                            .build());
                }
            }

            return metrics;
        } catch (Exception e) {
            log.error("Failed to get service metrics: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private ServiceMetricResponse queryServiceMetrics(String serviceId, String serviceName, String duration) {
        String metricsQuery = "query queryServiceMetrics($serviceId: ID!, $duration: Duration!) { "
                + "service: readMetricsValues(condition: {name: \"service_cpm\", entity: {scope: Service, serviceName: $serviceId}}, duration: $duration) { values { value } } "
                + "serviceRespTime: readMetricsValues(condition: {name: \"service_resp_time\", entity: {scope: Service, serviceName: $serviceId}}, duration: $duration) { values { value } } "
                + "serviceSLA: readMetricsValues(condition: {name: \"service_sla\", entity: {scope: Service, serviceName: $serviceId}}, duration: $duration) { values { value } } "
                + "serviceP99: readMetricsValues(condition: {name: \"service_p99\", entity: {scope: Service, serviceName: $serviceId}}, duration: $duration) { values { value } } "
                + "}";

        Map<String, Object> vars = Map.of(
                "serviceId", serviceId,
                "duration", Map.of("start", getStartTime(duration), "end", getEndTime(), "step", duration)
        );

        JsonNode result = executeGraphQL(metricsQuery, vars);
        if (result == null || !result.has("data")) {
            return ServiceMetricResponse.builder()
                    .serviceName(serviceName)
                    .avgResponseTime(0).p99(0).throughput(0).errorRate(0)
                    .healthStatus("UNKNOWN").build();
        }

        JsonNode data = result.get("data");
        double throughput = extractLastValue(data, "service");
        double avgRespTime = extractLastValue(data, "serviceRespTime");
        double sla = extractLastValue(data, "serviceSLA");
        double p99 = extractLastValue(data, "serviceP99");
        double errorRate = sla > 0 ? (100 - sla) : 0;

        String healthStatus = "HEALTHY";
        if (errorRate > 10 || avgRespTime > 3000) {
            healthStatus = "UNHEALTHY";
        } else if (errorRate > 5 || avgRespTime > 1000) {
            healthStatus = "DEGRADED";
        }

        return ServiceMetricResponse.builder()
                .serviceName(serviceName)
                .avgResponseTime(avgRespTime)
                .p99(p99)
                .throughput(throughput)
                .errorRate(errorRate)
                .healthStatus(healthStatus)
                .build();
    }

    public TraceListResponse queryTraces(String serviceName, String startTime, String endTime, Integer minDuration, String traceId) {
        try {
            String tracesQuery = "query queryTraces($condition: TraceQueryCondition!) { "
                    + "queryBasicTraces(condition: $condition) { "
                    + "traces { traceId serviceCode endpointNames duration spanCount isError startTime } "
                    + "} }";

            Map<String, Object> condition = new HashMap<>();
            if (serviceName != null) {
                condition.put("serviceName", serviceName);
            }
            if (traceId != null) {
                condition.put("traceId", traceId);
            }
            condition.put("queryDuration", Map.of(
                    "start", startTime != null ? startTime : getStartTime("DAY"),
                    "end", endTime != null ? endTime : getEndTime(),
                    "step", "DAY"
            ));
            if (minDuration != null) {
                condition.put("minTraceDuration", minDuration);
            }
            condition.put("paging", Map.of("pageNum", 1, "pageSize", 50, "needTotal", true));

            Map<String, Object> vars = Map.of("condition", condition);
            JsonNode result = executeGraphQL(tracesQuery, vars);

            if (result == null || !result.has("data") || !result.get("data").has("queryBasicTraces")) {
                return TraceListResponse.builder().traces(Collections.emptyList()).build();
            }

            JsonNode traces = result.get("data").get("queryBasicTraces").get("traces");
            List<TraceListResponse.TraceItem> items = new ArrayList<>();

            for (JsonNode trace : traces) {
                String endpoint = "";
                if (trace.has("endpointNames") && trace.get("endpointNames").isArray() && !trace.get("endpointNames").isEmpty()) {
                    endpoint = trace.get("endpointNames").get(0).asText();
                }

                items.add(TraceListResponse.TraceItem.builder()
                        .traceId(trace.get("traceId").asText())
                        .serviceName(trace.get("serviceCode").asText())
                        .endpoint(endpoint)
                        .duration(trace.get("duration").asLong())
                        .spanCount(trace.get("spanCount").asInt())
                        .status(trace.get("isError").asBoolean() ? "ERROR" : "SUCCESS")
                        .startTime(String.valueOf(trace.get("startTime").asLong()))
                        .isError(trace.get("isError").asBoolean())
                        .build());
            }

            return TraceListResponse.builder().traces(items).build();
        } catch (Exception e) {
            log.error("Failed to query traces: {}", e.getMessage());
            return TraceListResponse.builder().traces(Collections.emptyList()).build();
        }
    }

    public TraceDetailResponse getTraceDetail(String traceId) {
        try {
            String detailQuery = "query queryTrace($traceId: ID!) { "
                    + "trace: queryTrace(traceId: $traceId) { "
                    + "spans { spanId parentSpanId operationName startTime duration isError serviceCode tags { key value } } "
                    + "} }";

            Map<String, Object> vars = Map.of("traceId", traceId);
            JsonNode result = executeGraphQL(detailQuery, vars);

            if (result == null || !result.has("data") || !result.get("data").has("trace")) {
                return TraceDetailResponse.builder().traceId(traceId).spans(Collections.emptyList()).build();
            }

            JsonNode spans = result.get("data").get("trace").get("spans");
            List<TraceDetailResponse.SpanDetail> spanDetails = new ArrayList<>();

            for (JsonNode span : spans) {
                Map<String, String> tags = new HashMap<>();
                if (span.has("tags") && span.get("tags").isArray()) {
                    for (JsonNode tag : span.get("tags")) {
                        tags.put(tag.get("key").asText(), tag.get("value").asText());
                    }
                }

                spanDetails.add(TraceDetailResponse.SpanDetail.builder()
                        .spanId(span.get("spanId").asText())
                        .parentSpanId(span.has("parentSpanId") ? span.get("parentSpanId").asText() : "")
                        .operationName(span.get("operationName").asText())
                        .startTime(span.get("startTime").asLong())
                        .duration(span.get("duration").asLong())
                        .status(span.get("isError").asBoolean() ? "ERROR" : "OK")
                        .serviceCode(span.get("serviceCode").asText())
                        .tags(tags)
                        .build());
            }

            return TraceDetailResponse.builder().traceId(traceId).spans(spanDetails).build();
        } catch (Exception e) {
            log.error("Failed to get trace detail for {}: {}", traceId, e.getMessage());
            return TraceDetailResponse.builder().traceId(traceId).spans(Collections.emptyList()).build();
        }
    }

    public TopologyResponse getServiceTopology(String duration) {
        try {
            String topologyQuery = "query queryTopology($duration: Duration!) { "
                    + "getTopology(duration: $duration) { "
                    + "nodes { id name type } "
                    + "calls { source target callCount errorCount } "
                    + "} }";

            Map<String, Object> vars = Map.of(
                    "duration", Map.of("start", getStartTime(duration), "end", getEndTime(), "step", duration)
            );
            JsonNode result = executeGraphQL(topologyQuery, vars);

            if (result == null || !result.has("data") || !result.get("data").has("getTopology")) {
                return TopologyResponse.builder().nodes(Collections.emptyList()).edges(Collections.emptyList()).build();
            }

            JsonNode topology = result.get("data").get("getTopology");
            List<TopologyResponse.TopologyNode> nodes = new ArrayList<>();
            List<TopologyResponse.TopologyEdge> edges = new ArrayList<>();

            if (topology.has("nodes")) {
                for (JsonNode node : topology.get("nodes")) {
                    nodes.add(TopologyResponse.TopologyNode.builder()
                            .name(node.get("name").asText())
                            .type(node.has("type") ? node.get("type").asText() : "UNKNOWN")
                            .healthStatus("HEALTHY")
                            .build());
                }
            }

            if (topology.has("calls")) {
                for (JsonNode call : topology.get("calls")) {
                    long callCount = call.get("callCount").asLong();
                    long errorCount = call.has("errorCount") ? call.get("errorCount").asLong() : 0;
                    double errorRate = callCount > 0 ? (errorCount * 100.0 / callCount) : 0;

                    edges.add(TopologyResponse.TopologyEdge.builder()
                            .source(call.get("source").asText())
                            .target(call.get("target").asText())
                            .callCount(callCount)
                            .errorRate(Math.round(errorRate * 100.0) / 100.0)
                            .build());
                }
            }

            return TopologyResponse.builder().nodes(nodes).edges(edges).build();
        } catch (Exception e) {
            log.error("Failed to get service topology: {}", e.getMessage());
            return TopologyResponse.builder().nodes(Collections.emptyList()).edges(Collections.emptyList()).build();
        }
    }

    private JsonNode executeGraphQL(String query, Map<String, Object> variables) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("query", query);
            body.put("variables", variables);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            String response = restTemplate.postForObject(oapUrl + "/graphql", entity, String.class);
            if (response == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response);
            if (root.has("errors")) {
                log.warn("GraphQL errors: {}", root.get("errors"));
            }
            return root;
        } catch (Exception e) {
            log.error("GraphQL request failed: {}", e.getMessage());
            return null;
        }
    }

    private double extractLastValue(JsonNode data, String fieldName) {
        try {
            if (!data.has(fieldName)) {
                return 0;
            }
            JsonNode values = data.get(fieldName).get("values");
            if (values == null || !values.isArray() || values.isEmpty()) {
                return 0;
            }
            JsonNode lastValue = values.get(values.size() - 1);
            return lastValue.has("value") ? lastValue.get("value").asDouble() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getStartTime(String duration) {
        return switch (duration.toUpperCase()) {
            case "HOUR" -> java.time.LocalDateTime.now().minusHours(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case "DAY" -> java.time.LocalDateTime.now().minusDays(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case "WEEK" -> java.time.LocalDateTime.now().minusWeeks(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case "MONTH" -> java.time.LocalDateTime.now().minusMonths(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            default -> java.time.LocalDateTime.now().minusDays(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        };
    }

    private String getEndTime() {
        return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
