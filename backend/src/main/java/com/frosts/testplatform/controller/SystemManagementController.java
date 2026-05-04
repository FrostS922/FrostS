package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.system.CreateOrganizationUnitRequest;
import com.frosts.testplatform.dto.system.CreateRoleRequest;
import com.frosts.testplatform.dto.system.CreateUserRequest;
import com.frosts.testplatform.dto.system.OrganizationUnitResponse;
import com.frosts.testplatform.dto.system.PermissionResponse;
import com.frosts.testplatform.dto.system.ResetPasswordResponse;
import com.frosts.testplatform.dto.system.RoleResponse;
import com.frosts.testplatform.dto.system.SystemOverviewResponse;
import com.frosts.testplatform.dto.system.SystemSettingResponse;
import com.frosts.testplatform.dto.system.UpdateOrganizationUnitRequest;
import com.frosts.testplatform.dto.system.UpdateRoleRequest;
import com.frosts.testplatform.dto.system.UpdateRoleSortRequest;
import com.frosts.testplatform.dto.system.UpdateSystemSettingsRequest;
import com.frosts.testplatform.dto.system.UpdateUserRequest;
import com.frosts.testplatform.dto.system.UserResponse;
import com.frosts.testplatform.service.SystemManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "系统管理", description = "用户、角色、组织、设置管理")
public class SystemManagementController {

    private final SystemManagementService systemManagementService;

    @GetMapping("/overview")
    @Operation(summary = "获取系统概览")
    public ResponseEntity<ApiResponse<SystemOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.getOverview()));
    }

    @GetMapping("/users")
    @Operation(summary = "分页查询用户列表")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String search,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") int size) {
        Page<UserResponse> users = systemManagementService.getUsers(search,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(users.getContent(), users.getTotalElements()));
    }

    @PostMapping("/users")
    @Operation(summary = "创建用户")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.createUser(request)));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "更新用户")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable @Parameter(description = "用户ID") Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.updateUser(id, request)));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "删除用户")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable @Parameter(description = "用户ID") Long id, Authentication authentication) {
        systemManagementService.deleteUser(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/users/{id}/reset-password")
    @Operation(summary = "重置用户密码")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@PathVariable @Parameter(description = "用户ID") Long id) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.resetPassword(id)));
    }

    @PostMapping("/users/{id}/unlock")
    @Operation(summary = "解锁用户")
    public ResponseEntity<ApiResponse<Void>> unlockUser(@PathVariable @Parameter(description = "用户ID") Long id) {
        systemManagementService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/roles")
    @Operation(summary = "分页查询角色列表")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles(
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String search,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") int size) {
        Page<RoleResponse> roles = systemManagementService.getRoles(search,
                PageRequest.of(page, size, Sort.by("sortOrder").ascending().and(Sort.by("createdAt").descending())));
        return ResponseEntity.ok(ApiResponse.success(roles.getContent(), roles.getTotalElements()));
    }

    @PostMapping("/roles")
    @Operation(summary = "创建角色")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.createRole(request)));
    }

    @PutMapping("/roles/sort")
    @Operation(summary = "更新角色排序")
    public ResponseEntity<ApiResponse<Void>> updateRoleSort(@Valid @RequestBody UpdateRoleSortRequest request) {
        systemManagementService.updateRoleSortOrder(request.roleIds());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/roles/{id}")
    @Operation(summary = "更新角色")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable @Parameter(description = "角色ID") Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.updateRole(id, request)));
    }

    @DeleteMapping("/roles/{id}")
    @Operation(summary = "删除角色")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable @Parameter(description = "角色ID") Long id) {
        systemManagementService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/permissions")
    @Operation(summary = "获取权限列表")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions() {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.getPermissions()));
    }

    @GetMapping("/organizations/tree")
    @Operation(summary = "获取组织架构树")
    public ResponseEntity<ApiResponse<List<OrganizationUnitResponse>>> getOrganizationTree() {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.getOrganizationTree()));
    }

    @PostMapping("/organizations")
    @Operation(summary = "创建组织单元")
    public ResponseEntity<ApiResponse<OrganizationUnitResponse>> createOrganization(
            @Valid @RequestBody CreateOrganizationUnitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.createOrganizationUnit(request)));
    }

    @PutMapping("/organizations/{id}")
    @Operation(summary = "更新组织单元")
    public ResponseEntity<ApiResponse<OrganizationUnitResponse>> updateOrganization(
            @PathVariable @Parameter(description = "组织ID") Long id,
            @Valid @RequestBody UpdateOrganizationUnitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.updateOrganizationUnit(id, request)));
    }

    @DeleteMapping("/organizations/{id}")
    @Operation(summary = "删除组织单元")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable @Parameter(description = "组织ID") Long id) {
        systemManagementService.deleteOrganizationUnit(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/settings")
    @Operation(summary = "获取系统设置")
    public ResponseEntity<ApiResponse<List<SystemSettingResponse>>> getSettings(
            @RequestParam(required = false) @Parameter(description = "设置分类") String category) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.getSystemSettings(category)));
    }

    @PutMapping("/settings")
    @Operation(summary = "更新系统设置")
    public ResponseEntity<ApiResponse<List<SystemSettingResponse>>> updateSettings(
            @Valid @RequestBody UpdateSystemSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.updateSystemSettings(request)));
    }

    @PostMapping("/settings/reset")
    @Operation(summary = "重置系统设置")
    public ResponseEntity<ApiResponse<List<SystemSettingResponse>>> resetSettings(
            @RequestParam(required = false) @Parameter(description = "设置分类") String category) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.resetSystemSettings(category)));
    }
}
