package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.apm.ServiceMetricResponse;
import com.frosts.testplatform.dto.apm.TopologyResponse;
import com.frosts.testplatform.dto.apm.TraceDetailResponse;
import com.frosts.testplatform.dto.apm.TraceListResponse;
import com.frosts.testplatform.service.SkyWalkingApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apm")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "APM监控", description = "应用性能监控与链路追踪")
public class ApmController {

    private final SkyWalkingApiClient skyWalkingApiClient;

    @GetMapping("/services")
    @Operation(summary = "获取服务列表与指标")
    public ResponseEntity<ApiResponse<List<ServiceMetricResponse>>> getServices(
            @RequestParam(defaultValue = "DAY") @Parameter(description = "时间范围") String duration) {
        return ResponseEntity.ok(ApiResponse.success(skyWalkingApiClient.getServiceMetrics(duration)));
    }

    @GetMapping("/service/{name}/metrics")
    @Operation(summary = "获取指定服务指标")
    public ResponseEntity<ApiResponse<ServiceMetricResponse>> getServiceMetrics(
            @PathVariable @Parameter(description = "服务名称") String name,
            @RequestParam(defaultValue = "DAY") @Parameter(description = "时间范围") String duration) {
        List<ServiceMetricResponse> services = skyWalkingApiClient.getServiceMetrics(duration);
        ServiceMetricResponse result = services.stream()
                .filter(s -> name.equals(s.getServiceName()))
                .findFirst()
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/traces")
    @Operation(summary = "查询链路追踪列表")
    public ResponseEntity<ApiResponse<TraceListResponse>> queryTraces(
            @RequestParam(required = false) @Parameter(description = "服务名称") String serviceName,
            @RequestParam(required = false) @Parameter(description = "开始时间") String startTime,
            @RequestParam(required = false) @Parameter(description = "结束时间") String endTime,
            @RequestParam(required = false) @Parameter(description = "最小耗时(ms)") Integer minDuration,
            @RequestParam(required = false) @Parameter(description = "追踪ID") String traceId) {
        return ResponseEntity.ok(ApiResponse.success(
                skyWalkingApiClient.queryTraces(serviceName, startTime, endTime, minDuration, traceId)));
    }

    @GetMapping("/traces/{traceId}")
    @Operation(summary = "获取链路追踪详情")
    public ResponseEntity<ApiResponse<TraceDetailResponse>> getTraceDetail(@PathVariable @Parameter(description = "追踪ID") String traceId) {
        return ResponseEntity.ok(ApiResponse.success(skyWalkingApiClient.getTraceDetail(traceId)));
    }

    @GetMapping("/topology")
    @Operation(summary = "获取服务拓扑图")
    public ResponseEntity<ApiResponse<TopologyResponse>> getTopology(
            @RequestParam(defaultValue = "DAY") @Parameter(description = "时间范围") String duration) {
        return ResponseEntity.ok(ApiResponse.success(skyWalkingApiClient.getServiceTopology(duration)));
    }
}
