package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.dictionary.*;
import com.frosts.testplatform.service.DictionaryExcelService;
import com.frosts.testplatform.service.DictionaryPermissionService;
import com.frosts.testplatform.service.DictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dictionary")
@RequiredArgsConstructor
@Tag(name = "数据字典", description = "字典分类与枚举值管理")
public class DictionaryController {

    private final DictionaryService dictionaryService;
    private final DictionaryExcelService excelService;
    private final DictionaryPermissionService permissionService;

    @GetMapping("/types")
    @Operation(summary = "获取字典分类树")
    public ResponseEntity<ApiResponse<List<DictionaryTypeResponse>>> getTypeTree() {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.getTypeTree()));
    }

    @GetMapping("/types/{id}")
    @Operation(summary = "获取字典分类详情")
    public ResponseEntity<ApiResponse<DictionaryTypeResponse>> getTypeById(@PathVariable @Parameter(description = "分类ID") Long id) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.getTypeById(id)));
    }

    @PostMapping("/types")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建字典分类")
    public ResponseEntity<ApiResponse<DictionaryTypeResponse>> createType(
            @Valid @RequestBody CreateDictionaryTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.createType(request)));
    }

    @PutMapping("/types/{id}")
    @Operation(summary = "更新字典分类")
    public ResponseEntity<ApiResponse<DictionaryTypeResponse>> updateType(
            @PathVariable @Parameter(description = "分类ID") Long id,
            @Valid @RequestBody UpdateDictionaryTypeRequest request) {
        if (!permissionService.hasAdminPermission(id)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "没有权限修改此分类"));
        }
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.updateType(id, request)));
    }

    @DeleteMapping("/types/{id}")
    @Operation(summary = "删除字典分类")
    public ResponseEntity<ApiResponse<Void>> deleteType(@PathVariable @Parameter(description = "分类ID") Long id) {
        if (!permissionService.hasAdminPermission(id)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "没有权限删除此分类"));
        }
        dictionaryService.deleteType(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/types/{typeId}/items")
    @Operation(summary = "分页查询枚举值列表")
    public ResponseEntity<ApiResponse<List<DictionaryItemResponse>>> getItems(
            @PathVariable @Parameter(description = "分类ID") Long typeId,
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String keyword,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") int size) {
        if (!permissionService.hasReadPermission(typeId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "没有权限查看此分类"));
        }
        Page<DictionaryItemResponse> items = dictionaryService.getItems(typeId, keyword,
                PageRequest.of(page, size, Sort.by("sortOrder").ascending()));
        return ResponseEntity.ok(ApiResponse.success(items.getContent(), items.getTotalElements()));
    }

    @GetMapping("/types/code/{typeCode}/items")
    @Operation(summary = "按分类编码查询枚举值")
    public ResponseEntity<ApiResponse<List<DictionaryItemResponse>>> getItemsByTypeCode(
            @PathVariable @Parameter(description = "分类编码") String typeCode) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.getItemsByTypeCode(typeCode)));
    }

    @PostMapping("/items")
    @Operation(summary = "创建枚举值")
    public ResponseEntity<ApiResponse<DictionaryItemResponse>> createItem(
            @Valid @RequestBody CreateDictionaryItemRequest request) {
        if (!permissionService.hasWritePermission(request.typeId())) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "没有权限添加枚举值"));
        }
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.createItem(request)));
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "更新枚举值")
    public ResponseEntity<ApiResponse<DictionaryItemResponse>> updateItem(
            @PathVariable @Parameter(description = "枚举值ID") Long id,
            @Valid @RequestBody UpdateDictionaryItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.updateItem(id, request)));
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "删除枚举值")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable @Parameter(description = "枚举值ID") Long id) {
        dictionaryService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/items/{id}/status")
    @Operation(summary = "更新枚举值启用状态")
    public ResponseEntity<ApiResponse<DictionaryItemResponse>> updateItemStatus(
            @PathVariable @Parameter(description = "枚举值ID") Long id,
            @RequestParam @Parameter(description = "是否启用") boolean enabled) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.updateItemStatus(id, enabled)));
    }

    @GetMapping("/types/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取分类权限列表")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTypePermissions(@PathVariable @Parameter(description = "分类ID") Long id) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.getTypePermissions(id)));
    }

    @PutMapping("/types/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "设置分类权限")
    public ResponseEntity<ApiResponse<Void>> setTypePermissions(
            @PathVariable @Parameter(description = "分类ID") Long id,
            @Valid @RequestBody DictionaryTypePermissionRequest request) {
        dictionaryService.setTypePermissions(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "导入字典Excel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importExcel(@RequestParam("file") @Parameter(description = "Excel文件") MultipartFile file) {
        try {
            Map<String, Object> result = excelService.importFromExcel(file);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "导入失败: " + e.getMessage()));
        }
    }

    @GetMapping("/export")
    @Operation(summary = "导出字典Excel")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            byte[] data = excelService.exportToExcel();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dictionary_export.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "查询字典操作日志")
    public ResponseEntity<ApiResponse<List<DictionaryLogResponse>>> getLogs(
            @RequestParam(required = false) @Parameter(description = "分类ID") Long typeId,
            @RequestParam(required = false) @Parameter(description = "操作人") String operator,
            @RequestParam(required = false) @Parameter(description = "开始时间") LocalDateTime startTime,
            @RequestParam(required = false) @Parameter(description = "结束时间") LocalDateTime endTime,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页数量") int size) {
        Page<DictionaryLogResponse> logs = dictionaryService.getLogs(typeId, operator, startTime, endTime,
                PageRequest.of(page, size, Sort.by("operatedAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(logs.getContent(), logs.getTotalElements()));
    }
}
