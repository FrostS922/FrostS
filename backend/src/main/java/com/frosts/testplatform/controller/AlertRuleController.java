package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.alertrule.AlertRulePreviewResponse;
import com.frosts.testplatform.dto.alertrule.AlertRuleResponse;
import com.frosts.testplatform.dto.alertrule.CreateAlertRuleRequest;
import com.frosts.testplatform.dto.alertrule.UpdateAlertRuleRequest;
import com.frosts.testplatform.service.AlertRuleService;
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
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertRuleResponse>>> getAllRules() {
        List<AlertRuleResponse> rules = alertRuleService.getAllRules();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AlertRuleResponse>> createRule(@Valid @RequestBody CreateAlertRuleRequest request) {
        AlertRuleResponse response = alertRuleService.createRule(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> updateRule(@PathVariable Long id, @RequestBody UpdateAlertRuleRequest request) {
        AlertRuleResponse response = alertRuleService.updateRule(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> toggleRule(@PathVariable Long id) {
        AlertRuleResponse response = alertRuleService.toggleRule(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<AlertRulePreviewResponse>> previewRule(@RequestBody CreateAlertRuleRequest request) {
        AlertRulePreviewResponse response = alertRuleService.previewRule(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
