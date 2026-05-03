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
public class SystemManagementController {

    private final SystemManagementService systemManagementService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<SystemOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.getOverview()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UserResponse> users = systemManagementService.getUsers(search,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(users.getContent(), users.getTotalElements()));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.createUser(request)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.updateUser(id, request)));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id, Authentication authentication) {
        systemManagementService.deleteUser(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.resetPassword(id)));
    }

    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockUser(@PathVariable Long id) {
        systemManagementService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<RoleResponse> roles = systemManagementService.getRoles(search,
                PageRequest.of(page, size, Sort.by("sortOrder").ascending().and(Sort.by("createdAt").descending())));
        return ResponseEntity.ok(ApiResponse.success(roles.getContent(), roles.getTotalElements()));
    }

    @PostMapping("/roles")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.createRole(request)));
    }

    @PutMapping("/roles/sort")
    public ResponseEntity<ApiResponse<Void>> updateRoleSort(@Valid @RequestBody UpdateRoleSortRequest request) {
        systemManagementService.updateRoleSortOrder(request.roleIds());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.updateRole(id, request)));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        systemManagementService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions() {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.getPermissions()));
    }

    @GetMapping("/organizations/tree")
    public ResponseEntity<ApiResponse<List<OrganizationUnitResponse>>> getOrganizationTree() {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.getOrganizationTree()));
    }

    @PostMapping("/organizations")
    public ResponseEntity<ApiResponse<OrganizationUnitResponse>> createOrganization(
            @Valid @RequestBody CreateOrganizationUnitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.createOrganizationUnit(request)));
    }

    @PutMapping("/organizations/{id}")
    public ResponseEntity<ApiResponse<OrganizationUnitResponse>> updateOrganization(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrganizationUnitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.updateOrganizationUnit(id, request)));
    }

    @DeleteMapping("/organizations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable Long id) {
        systemManagementService.deleteOrganizationUnit(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<List<SystemSettingResponse>>> getSettings(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.getSystemSettings(category)));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<List<SystemSettingResponse>>> updateSettings(
            @Valid @RequestBody UpdateSystemSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.updateSystemSettings(request)));
    }

    @PostMapping("/settings/reset")
    public ResponseEntity<ApiResponse<List<SystemSettingResponse>>> resetSettings(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(systemManagementService.resetSystemSettings(category)));
    }
}
