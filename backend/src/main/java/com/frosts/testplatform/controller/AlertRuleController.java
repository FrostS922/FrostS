package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.alertrule.AlertRulePreviewResponse;
import com.frosts.testplatform.dto.alertrule.AlertRuleResponse;
import com.frosts.testplatform.dto.alertrule.CreateAlertRuleRequest;
import com.frosts.testplatform.dto.alertrule.UpdateAlertRuleRequest;
import com.frosts.testplatform.service.AlertRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/alert-rules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "告警规则", description = "告警规则配置与管理")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @GetMapping
    @Operation(summary = "获取全部告警规则")
    public ResponseEntity<ApiResponse<List<AlertRuleResponse>>> getAllRules() {
        List<AlertRuleResponse> rules = alertRuleService.getAllRules();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @PostMapping
    @Operation(summary = "创建告警规则")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> createRule(@Valid @RequestBody CreateAlertRuleRequest request) {
        AlertRuleResponse response = alertRuleService.createRule(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新告警规则")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> updateRule(@PathVariable @Parameter(description = "规则ID") Long id, @RequestBody UpdateAlertRuleRequest request) {
        AlertRuleResponse response = alertRuleService.updateRule(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除告警规则")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable @Parameter(description = "规则ID") Long id) {
        alertRuleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "切换告警规则启用状态")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> toggleRule(@PathVariable @Parameter(description = "规则ID") Long id) {
        AlertRuleResponse response = alertRuleService.toggleRule(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/preview")
    @Operation(summary = "预览告警规则效果")
    public ResponseEntity<ApiResponse<AlertRulePreviewResponse>> previewRule(@RequestBody CreateAlertRuleRequest request) {
        AlertRulePreviewResponse response = alertRuleService.previewRule(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
