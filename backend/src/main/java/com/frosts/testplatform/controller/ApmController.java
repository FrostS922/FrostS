package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.apm.ServiceMetricResponse;
import com.frosts.testplatform.dto.apm.TopologyResponse;
import com.frosts.testplatform.dto.apm.TraceDetailResponse;
import com.frosts.testplatform.dto.apm.TraceListResponse;
import com.frosts.testplatform.service.SkyWalkingApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apm")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApmController {

    private final SkyWalkingApiClient skyWalkingApiClient;

    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<ServiceMetricResponse>>> getServices(
            @RequestParam(defaultValue = "DAY") String duration) {
        return ResponseEntity.ok(ApiResponse.success(skyWalkingApiClient.getServiceMetrics(duration)));
    }

    @GetMapping("/service/{name}/metrics")
    public ResponseEntity<ApiResponse<ServiceMetricResponse>> getServiceMetrics(
            @PathVariable String name,
            @RequestParam(defaultValue = "DAY") String duration) {
        List<ServiceMetricResponse> services = skyWalkingApiClient.getServiceMetrics(duration);
        ServiceMetricResponse result = services.stream()
                .filter(s -> name.equals(s.getServiceName()))
                .findFirst()
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/traces")
    public ResponseEntity<ApiResponse<TraceListResponse>> queryTraces(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer minDuration,
            @RequestParam(required = false) String traceId) {
        return ResponseEntity.ok(ApiResponse.success(
                skyWalkingApiClient.queryTraces(serviceName, startTime, endTime, minDuration, traceId)));
    }

    @GetMapping("/traces/{traceId}")
    public ResponseEntity<ApiResponse<TraceDetailResponse>> getTraceDetail(@PathVariable String traceId) {
        return ResponseEntity.ok(ApiResponse.success(skyWalkingApiClient.getTraceDetail(traceId)));
    }

    @GetMapping("/topology")
    public ResponseEntity<ApiResponse<TopologyResponse>> getTopology(
            @RequestParam(defaultValue = "DAY") String duration) {
        return ResponseEntity.ok(ApiResponse.success(skyWalkingApiClient.getServiceTopology(duration)));
    }
}
