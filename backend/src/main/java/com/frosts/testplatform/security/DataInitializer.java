package com.frosts.testplatform.security;

import com.frosts.testplatform.entity.Permission;
import com.frosts.testplatform.entity.OrganizationUnit;
import com.frosts.testplatform.entity.Role;
import com.frosts.testplatform.entity.SystemSetting;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.repository.OrganizationUnitRepository;
import com.frosts.testplatform.repository.PermissionRepository;
import com.frosts.testplatform.repository.RoleRepository;
import com.frosts.testplatform.repository.SystemSettingRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initPermissions();
        initRoles();
        initRolePermissions();
        initOrganization();
        initSystemSettings();
        initAdmin();
    }

    private void initPermissions() {
        createPermission("user:read", "查看用户", "sys:user", "read");
        createPermission("user:write", "管理用户", "sys:user", "write");
        createPermission("user:delete", "删除用户", "sys:user", "delete");
        createPermission("role:read", "查看角色", "sys:role", "read");
        createPermission("role:write", "管理角色", "sys:role", "write");
        createPermission("permission:read", "查看权限", "sys:permission", "read");
        createPermission("organization:read", "查看组织架构", "sys:organization", "read");
        createPermission("organization:write", "管理组织架构", "sys:organization", "write");
        createPermission("setting:read", "查看系统设置", "sys:setting", "read");
        createPermission("setting:write", "管理系统设置", "sys:setting", "write");
        createPermission("project:read", "查看项目", "project", "read");
        createPermission("project:write", "管理项目", "project", "write");
        createPermission("requirement:read", "查看需求", "requirement", "read");
        createPermission("requirement:write", "管理需求", "requirement", "write");
        createPermission("testcase:read", "查看用例", "testcase", "read");
        createPermission("testcase:write", "管理用例", "testcase", "write");
        createPermission("testplan:read", "查看计划", "testplan", "read");
        createPermission("testplan:write", "管理计划", "testplan", "write");
        createPermission("defect:read", "查看缺陷", "defect", "read");
        createPermission("defect:write", "管理缺陷", "defect", "write");
    }

    private void initRoles() {
        createRole("ADMIN", "系统管理员", "系统管理员，拥有所有权限");
        createRole("TEST_MANAGER", "测试经理", "测试经理，可管理测试计划、用例和缺陷");
        createRole("TEST_ENGINEER", "测试工程师", "测试工程师，可执行测试、提交缺陷");
        createRole("DEVELOPER", "开发人员", "开发人员，可查看需求和缺陷");
    }

    private void initRolePermissions() {
        Set<Permission> allPermissions = new HashSet<>(permissionRepository.findByIsDeletedFalseOrderByResourceAscCodeAsc());
        roleRepository.findByCode("ADMIN").ifPresent(role -> {
            role.setPermissions(allPermissions);
            roleRepository.save(role);
        });

        assignPermissions("TEST_MANAGER",
                "project:read", "project:write",
                "requirement:read", "requirement:write",
                "testcase:read", "testcase:write",
                "testplan:read", "testplan:write",
                "defect:read", "defect:write");
        assignPermissions("TEST_ENGINEER",
                "project:read",
                "requirement:read",
                "testcase:read", "testcase:write",
                "testplan:read", "testplan:write",
                "defect:read", "defect:write");
        assignPermissions("DEVELOPER",
                "project:read",
                "requirement:read",
                "defect:read", "defect:write");
    }

    private void initOrganization() {
        if (organizationUnitRepository.findByCode("FROSTS").isEmpty()) {
            OrganizationUnit root = new OrganizationUnit();
            root.setCode("FROSTS");
            root.setName("FrostS 测试平台");
            root.setType("COMPANY");
            root.setLeader("系统管理员");
            root.setEnabled(true);
            root.setSortOrder(0);
            root.setDescription("默认根组织");
            organizationUnitRepository.save(root);
        }
    }

    private void initSystemSettings() {
        createSetting("basic.site_name", "平台名称", "BASIC", "TEXT", "FrostS 测试平台",
                null, "显示在登录页和浏览器标题中的系统名称", 10, true);
        createSetting("basic.site_description", "平台描述", "BASIC", "TEXT",
                "企业级测试管理与质量协作平台", null, "用于系统说明和对外展示", 20, true);
        createSetting("basic.system_mode", "运行模式", "BASIC", "SELECT", "STANDARD",
                "STANDARD,STRICT", "标准模式适合日常协作，严格模式适合强流程管控", 30, true);
        createSetting("security.password_min_length", "最小密码长度", "SECURITY", "NUMBER", "6",
                null, "新建用户和重置密码时建议遵守的最小长度", 10, true);
        createSetting("security.session_timeout_minutes", "会话超时分钟", "SECURITY", "NUMBER", "120",
                null, "前端会话空闲超时建议值", 20, true);
        createSetting("security.login_max_attempts", "登录失败上限", "SECURITY", "NUMBER", "5",
                null, "账号锁定策略预留配置", 30, true);
        createSetting("notification.email_enabled", "启用邮件通知", "NOTIFICATION", "BOOLEAN", "false",
                null, "开启后可对缺陷和计划事件发送邮件", 10, true);
        createSetting("notification.smtp_host", "SMTP 服务地址", "NOTIFICATION", "TEXT", "",
                null, "邮件服务器主机名或 IP", 20, true);
        createSetting("notification.sender_email", "发件邮箱", "NOTIFICATION", "TEXT", "",
                null, "系统通知邮件的默认发件人", 30, true);
        createSetting("quality.require_case_review", "用例需要评审", "QUALITY", "BOOLEAN", "true",
                null, "启用后用例进入执行前应完成评审", 10, true);
        createSetting("quality.defect_auto_close_days", "缺陷自动关闭天数", "QUALITY", "NUMBER", "7",
                null, "已解决缺陷超过指定天数可自动关闭", 20, true);
        createSetting("quality.default_priority", "默认优先级", "QUALITY", "SELECT", "MEDIUM",
                "LOW,MEDIUM,HIGH,URGENT", "新建需求、用例或缺陷时的默认优先级", 30, true);
    }

    private void initAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setEmail("admin@frosts.com");
            admin.setEnabled(true);
            admin.setRoles(Set.of(
                    roleRepository.findByCode("ADMIN").orElseThrow()
            ));
            userRepository.save(admin);
        }
    }

    private void createPermission(String code, String name, String resource, String action) {
        Permission p = permissionRepository.findByCode(code).orElseGet(Permission::new);
        p.setCode(code);
        p.setName(name);
        p.setResource(resource);
        p.setAction(action);
        p.setIsDeleted(false);
        permissionRepository.save(p);
    }

    private void createRole(String code, String name, String description) {
        Role role = roleRepository.findByCode(code).orElseGet(Role::new);
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setIsDeleted(false);
        roleRepository.save(role);
    }

    private void createSetting(
            String settingKey,
            String name,
            String category,
            String valueType,
            String defaultValue,
            String options,
            String description,
            Integer sortOrder,
            Boolean editable) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(settingKey).orElseGet(SystemSetting::new);
        boolean isNew = setting.getId() == null;
        setting.setSettingKey(settingKey);
        setting.setName(name);
        setting.setCategory(category);
        setting.setValueType(valueType);
        setting.setDefaultValue(defaultValue);
        if (isNew) {
            setting.setSettingValue(defaultValue);
        }
        setting.setOptions(options);
        setting.setDescription(description);
        setting.setSortOrder(sortOrder);
        setting.setEditable(editable);
        setting.setIsDeleted(false);
        systemSettingRepository.save(setting);
    }

    private void assignPermissions(String roleCode, String... permissionCodes) {
        Map<String, Permission> permissionByCode = permissionRepository.findByIsDeletedFalseOrderByResourceAscCodeAsc()
                .stream()
                .collect(Collectors.toMap(Permission::getCode, Function.identity()));
        Set<Permission> permissions = Arrays.stream(permissionCodes)
                .map(permissionByCode::get)
                .collect(Collectors.toSet());

        roleRepository.findByCode(roleCode).ifPresent(role -> {
            role.setPermissions(permissions);
            roleRepository.save(role);
        });
    }
}
