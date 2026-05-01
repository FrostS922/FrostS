package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.system.CreateRoleRequest;
import com.frosts.testplatform.dto.system.CreateUserRequest;
import com.frosts.testplatform.dto.system.CreateOrganizationUnitRequest;
import com.frosts.testplatform.dto.system.OrganizationUnitResponse;
import com.frosts.testplatform.dto.system.PermissionResponse;
import com.frosts.testplatform.dto.system.ResetPasswordRequest;
import com.frosts.testplatform.dto.system.RoleResponse;
import com.frosts.testplatform.dto.system.RoleSummaryResponse;
import com.frosts.testplatform.dto.system.SystemOverviewResponse;
import com.frosts.testplatform.dto.system.SystemSettingResponse;
import com.frosts.testplatform.dto.system.UpdateOrganizationUnitRequest;
import com.frosts.testplatform.dto.system.UpdateRoleRequest;
import com.frosts.testplatform.dto.system.UpdateSystemSettingRequest;
import com.frosts.testplatform.dto.system.UpdateSystemSettingsRequest;
import com.frosts.testplatform.dto.system.UpdateUserRequest;
import com.frosts.testplatform.dto.system.UserResponse;
import com.frosts.testplatform.entity.OrganizationUnit;
import com.frosts.testplatform.entity.Permission;
import com.frosts.testplatform.entity.Role;
import com.frosts.testplatform.entity.SystemSetting;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.repository.OrganizationUnitRepository;
import com.frosts.testplatform.repository.PermissionRepository;
import com.frosts.testplatform.repository.RoleRepository;
import com.frosts.testplatform.repository.SystemSettingRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public SystemOverviewResponse getOverview() {
        long totalUsers = userRepository.countByIsDeletedFalse();
        long enabledUsers = userRepository.countByEnabledAndIsDeletedFalse(true);
        return new SystemOverviewResponse(
                totalUsers,
                enabledUsers,
                totalUsers - enabledUsers,
                roleRepository.countByIsDeletedFalse(),
                permissionRepository.countByIsDeletedFalse(),
                organizationUnitRepository.countByIsDeletedFalse(),
                systemSettingRepository.countByIsDeletedFalse()
        );
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsers(String search, Pageable pageable) {
        String keyword = normalize(search);
        Page<User> users = keyword == null
                ? userRepository.findByIsDeletedFalse(pageable)
                : userRepository.searchActiveUsers(keyword, pageable);
        return users.map(this::toUserResponse);
    }

    public UserResponse createUser(CreateUserRequest request) {
        String username = requireText(request.username(), "用户名不能为空");
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在: " + username);
        }

        String email = normalize(request.email());
        if (email != null && userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已存在: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(normalize(request.realName()));
        user.setEmail(email);
        user.setPhone(normalize(request.phone()));
        user.setEnabled(request.enabled() == null || request.enabled());
        user.setRoles(resolveRoles(request.roleIds()));

        return toUserResponse(userRepository.save(user));
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findActiveUser(id);

        String username = normalize(request.username());
        if (username != null && !Objects.equals(username, user.getUsername())) {
            userRepository.findByUsername(username)
                    .filter(existing -> !Objects.equals(existing.getId(), id))
                    .ifPresent(existing -> {
                        throw new RuntimeException("用户名已存在: " + username);
                    });
            user.setUsername(username);
        }

        String email = normalize(request.email());
        if (email != null && !Objects.equals(email, user.getEmail()) && userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已存在: " + email);
        }

        user.setRealName(normalize(request.realName()));
        user.setEmail(email);
        user.setPhone(normalize(request.phone()));
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.roleIds() != null) {
            user.setRoles(resolveRoles(request.roleIds()));
        }

        return toUserResponse(userRepository.save(user));
    }

    public void deleteUser(Long id, String currentUsername) {
        User user = findActiveUser(id);
        if (Objects.equals(user.getUsername(), currentUsername)) {
            throw new RuntimeException("不能删除当前登录用户");
        }
        user.setEnabled(false);
        user.setIsDeleted(true);
        userRepository.save(user);
    }

    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = findActiveUser(id);
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<RoleResponse> getRoles(String search, Pageable pageable) {
        String keyword = normalize(search);
        Page<Role> roles = keyword == null
                ? roleRepository.findByIsDeletedFalse(pageable)
                : roleRepository.searchActiveRoles(keyword, pageable);
        return roles.map(this::toRoleResponse);
    }

    public RoleResponse createRole(CreateRoleRequest request) {
        String code = requireText(request.code(), "角色编码不能为空").toUpperCase();
        if (roleRepository.existsByCode(code)) {
            throw new RuntimeException("角色编码已存在: " + code);
        }

        Role role = new Role();
        role.setCode(code);
        role.setName(requireText(request.name(), "角色名称不能为空"));
        role.setDescription(normalize(request.description()));
        role.setPermissions(resolvePermissions(request.permissionIds()));

        return toRoleResponse(roleRepository.save(role));
    }

    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        Role role = findActiveRole(id);
        String code = requireText(request.code(), "角色编码不能为空").toUpperCase();

        if ("ADMIN".equals(role.getCode()) && !"ADMIN".equals(code)) {
            throw new RuntimeException("内置管理员角色编码不能修改");
        }

        roleRepository.findByCode(code)
                .filter(existing -> !Objects.equals(existing.getId(), id))
                .ifPresent(existing -> {
                    throw new RuntimeException("角色编码已存在: " + code);
                });

        role.setCode(code);
        role.setName(requireText(request.name(), "角色名称不能为空"));
        role.setDescription(normalize(request.description()));
        role.setPermissions(resolvePermissions(request.permissionIds()));

        return toRoleResponse(roleRepository.save(role));
    }

    public void deleteRole(Long id) {
        Role role = findActiveRole(id);
        if ("ADMIN".equals(role.getCode())) {
            throw new RuntimeException("内置管理员角色不能删除");
        }
        if (userRepository.countByRoles_IdAndIsDeletedFalse(id) > 0) {
            throw new RuntimeException("角色已分配给用户，不能删除");
        }
        role.setIsDeleted(true);
        roleRepository.save(role);
    }

    public void updateRoleSortOrder(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<Role> roles = roleRepository.findAllById(roleIds);
        Map<Long, Role> roleMap = roles.stream().collect(Collectors.toMap(Role::getId, role -> role));
        for (int i = 0; i < roleIds.size(); i++) {
            Role role = roleMap.get(roleIds.get(i));
            if (role != null) {
                role.setSortOrder(i);
                roleRepository.save(role);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions() {
        return permissionRepository.findByIsDeletedFalseOrderByResourceAscCodeAsc()
                .stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationUnitResponse> getOrganizationTree() {
        List<OrganizationUnit> units = organizationUnitRepository.findByIsDeletedFalseOrderBySortOrderAscCreatedAtAsc();
        Set<Long> activeIds = units.stream()
                .map(OrganizationUnit::getId)
                .collect(Collectors.toSet());
        Map<Long, List<OrganizationUnit>> childrenByParentId = units.stream()
                .filter(unit -> unit.getParent() != null && activeIds.contains(unit.getParent().getId()))
                .collect(Collectors.groupingBy(unit -> unit.getParent().getId()));

        return units.stream()
                .filter(unit -> unit.getParent() == null || !activeIds.contains(unit.getParent().getId()))
                .map(unit -> toOrganizationResponse(unit, childrenByParentId))
                .toList();
    }

    public OrganizationUnitResponse createOrganizationUnit(CreateOrganizationUnitRequest request) {
        String code = requireText(request.code(), "组织编码不能为空").toUpperCase();
        if (organizationUnitRepository.existsByCode(code)) {
            throw new RuntimeException("组织编码已存在: " + code);
        }

        OrganizationUnit unit = new OrganizationUnit();
        applyOrganizationValues(unit, request.parentId(), request.name(), code, request.type(), request.leader(),
                request.contactEmail(), request.contactPhone(), request.sortOrder(), request.enabled(), request.description());

        return toOrganizationResponse(organizationUnitRepository.save(unit), Map.of());
    }

    public OrganizationUnitResponse updateOrganizationUnit(Long id, UpdateOrganizationUnitRequest request) {
        OrganizationUnit unit = findActiveOrganizationUnit(id);
        String code = requireText(request.code(), "组织编码不能为空").toUpperCase();

        organizationUnitRepository.findByCode(code)
                .filter(existing -> !Objects.equals(existing.getId(), id))
                .ifPresent(existing -> {
                    throw new RuntimeException("组织编码已存在: " + code);
                });

        OrganizationUnit parent = resolveOrganizationParent(request.parentId());
        ensureNotSelfOrDescendant(id, parent);
        applyOrganizationValues(unit, parent, request.name(), code, request.type(), request.leader(),
                request.contactEmail(), request.contactPhone(), request.sortOrder(), request.enabled(), request.description());

        return toOrganizationResponse(organizationUnitRepository.save(unit), Map.of());
    }

    public void deleteOrganizationUnit(Long id) {
        OrganizationUnit unit = findActiveOrganizationUnit(id);
        if (organizationUnitRepository.countByParent_IdAndIsDeletedFalse(id) > 0) {
            throw new RuntimeException("存在下级组织，不能删除");
        }
        unit.setEnabled(false);
        unit.setIsDeleted(true);
        organizationUnitRepository.save(unit);
    }

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> getSystemSettings(String category) {
        String normalizedCategory = normalize(category);
        List<SystemSetting> settings = normalizedCategory == null
                ? systemSettingRepository.findByIsDeletedFalseOrderByCategoryAscSortOrderAsc()
                : systemSettingRepository.findByCategoryAndIsDeletedFalseOrderBySortOrderAsc(normalizedCategory.toUpperCase());
        return settings.stream()
                .map(this::toSystemSettingResponse)
                .toList();
    }

    public List<SystemSettingResponse> updateSystemSettings(UpdateSystemSettingsRequest request) {
        List<String> settingKeys = request.settings().stream()
                .map(UpdateSystemSettingRequest::settingKey)
                .map(this::requireSettingKey)
                .toList();

        List<SystemSettingResponse> responses = request.settings().stream()
                .map(item -> updateSystemSetting(requireSettingKey(item.settingKey()), item.settingValue()))
                .toList();

        return systemSettingRepository.findByIsDeletedFalseOrderByCategoryAscSortOrderAsc()
                .stream()
                .filter(setting -> settingKeys.contains(setting.getSettingKey()))
                .map(this::toSystemSettingResponse)
                .map(response -> responses.stream()
                        .filter(updated -> Objects.equals(updated.settingKey(), response.settingKey()))
                        .findFirst()
                        .orElse(response))
                .toList();
    }

    public List<SystemSettingResponse> resetSystemSettings(String category) {
        List<SystemSetting> settings = normalize(category) == null
                ? systemSettingRepository.findByIsDeletedFalseOrderByCategoryAscSortOrderAsc()
                : systemSettingRepository.findByCategoryAndIsDeletedFalseOrderBySortOrderAsc(category.trim().toUpperCase());

        settings.forEach(setting -> {
            if (Boolean.TRUE.equals(setting.getEditable())) {
                setting.setSettingValue(setting.getDefaultValue());
                systemSettingRepository.save(setting);
            }
        });

        return settings.stream()
                .map(this::toSystemSettingResponse)
                .toList();
    }

    private User findActiveUser(Long id) {
        return userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
    }

    private Role findActiveRole(Long id) {
        return roleRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("角色不存在: " + id));
    }

    private OrganizationUnit findActiveOrganizationUnit(Long id) {
        return organizationUnitRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("组织不存在: " + id));
    }

    private SystemSetting findActiveSystemSetting(String settingKey) {
        return systemSettingRepository.findBySettingKeyAndIsDeletedFalse(settingKey)
                .orElseThrow(() -> new RuntimeException("系统设置不存在: " + settingKey));
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new RuntimeException("包含不存在的角色");
        }
        return new HashSet<>(roles);
    }

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new RuntimeException("包含不存在的权限");
        }
        return new HashSet<>(permissions);
    }

    private UserResponse toUserResponse(User user) {
        List<RoleSummaryResponse> roles = user.getRoles().stream()
                .sorted(Comparator.comparing(role -> valueOrEmpty(role.getCode())))
                .map(role -> new RoleSummaryResponse(role.getId(), role.getCode(), role.getName()))
                .toList();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getEnabled(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private RoleResponse toRoleResponse(Role role) {
        List<PermissionResponse> permissions = role.getPermissions().stream()
                .sorted(Comparator.comparing((Permission permission) -> valueOrEmpty(permission.getResource()))
                        .thenComparing(permission -> valueOrEmpty(permission.getCode())))
                .map(this::toPermissionResponse)
                .toList();

        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getSortOrder(),
                permissions,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getDescription(),
                permission.getResource(),
                permission.getAction(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }

    private OrganizationUnitResponse toOrganizationResponse(
            OrganizationUnit unit,
            Map<Long, List<OrganizationUnit>> childrenByParentId) {
        Long parentId = unit.getParent() == null ? null : unit.getParent().getId();
        List<OrganizationUnitResponse> children = childrenByParentId.getOrDefault(unit.getId(), List.of())
                .stream()
                .sorted(Comparator.comparing(OrganizationUnit::getSortOrder)
                        .thenComparing(organizationUnit -> valueOrEmpty(organizationUnit.getName())))
                .map(child -> toOrganizationResponse(child, childrenByParentId))
                .toList();

        return new OrganizationUnitResponse(
                unit.getId(),
                parentId,
                unit.getCode(),
                unit.getName(),
                unit.getType(),
                unit.getLeader(),
                unit.getContactEmail(),
                unit.getContactPhone(),
                unit.getSortOrder(),
                unit.getEnabled(),
                unit.getDescription(),
                children,
                unit.getCreatedAt(),
                unit.getUpdatedAt()
        );
    }

    private SystemSettingResponse updateSystemSetting(String settingKey, String rawValue) {
        SystemSetting setting = findActiveSystemSetting(settingKey);
        if (!Boolean.TRUE.equals(setting.getEditable())) {
            throw new RuntimeException("系统设置不可编辑: " + settingKey);
        }
        String value = normalizeSettingValue(setting, rawValue);
        setting.setSettingValue(value);
        return toSystemSettingResponse(systemSettingRepository.save(setting));
    }

    private SystemSettingResponse toSystemSettingResponse(SystemSetting setting) {
        return new SystemSettingResponse(
                setting.getId(),
                setting.getSettingKey(),
                setting.getSettingValue(),
                setting.getDefaultValue(),
                setting.getName(),
                setting.getCategory(),
                setting.getValueType(),
                parseOptions(setting.getOptions()),
                setting.getDescription(),
                setting.getSortOrder(),
                setting.getEditable(),
                setting.getUpdatedAt()
        );
    }

    private void applyOrganizationValues(
            OrganizationUnit unit,
            Long parentId,
            String name,
            String code,
            String type,
            String leader,
            String contactEmail,
            String contactPhone,
            Integer sortOrder,
            Boolean enabled,
            String description) {
        applyOrganizationValues(unit, resolveOrganizationParent(parentId), name, code, type, leader, contactEmail,
                contactPhone, sortOrder, enabled, description);
    }

    private void applyOrganizationValues(
            OrganizationUnit unit,
            OrganizationUnit parent,
            String name,
            String code,
            String type,
            String leader,
            String contactEmail,
            String contactPhone,
            Integer sortOrder,
            Boolean enabled,
            String description) {
        unit.setParent(parent);
        unit.setName(requireText(name, "组织名称不能为空"));
        unit.setCode(code);
        unit.setType(requireText(type, "组织类型不能为空").toUpperCase());
        unit.setLeader(normalize(leader));
        unit.setContactEmail(normalize(contactEmail));
        unit.setContactPhone(normalize(contactPhone));
        unit.setSortOrder(sortOrder == null ? 0 : sortOrder);
        unit.setEnabled(enabled == null || enabled);
        unit.setDescription(normalize(description));
    }

    private OrganizationUnit resolveOrganizationParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return findActiveOrganizationUnit(parentId);
    }

    private void ensureNotSelfOrDescendant(Long organizationId, OrganizationUnit parent) {
        OrganizationUnit current = parent;
        while (current != null) {
            if (Objects.equals(current.getId(), organizationId)) {
                throw new RuntimeException("不能将组织移动到自身或下级组织下");
            }
            current = current.getParent();
        }
    }

    private String normalizeSettingValue(SystemSetting setting, String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        String valueType = requireText(setting.getValueType(), "设置类型不能为空").toUpperCase();

        if ("BOOLEAN".equals(valueType)) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                throw new RuntimeException("布尔设置只能为 true 或 false: " + setting.getSettingKey());
            }
            return value.toLowerCase();
        }

        if ("NUMBER".equals(valueType)) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                throw new RuntimeException("数字设置格式不正确: " + setting.getSettingKey());
            }
            return value;
        }

        if ("SELECT".equals(valueType)) {
            List<String> options = parseOptions(setting.getOptions());
            if (!options.contains(value)) {
                throw new RuntimeException("设置值不在可选范围内: " + setting.getSettingKey());
            }
        }

        return value;
    }

    private List<String> parseOptions(String options) {
        String normalized = normalize(options);
        if (normalized == null) {
            return List.of();
        }
        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(option -> !option.isEmpty())
                .toList();
    }

    private String requireSettingKey(String settingKey) {
        return requireText(settingKey, "设置键不能为空");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireText(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new RuntimeException(message);
        }
        return normalized;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
