package com.frosts.testplatform.dto.system;

public record SystemOverviewResponse(
        long totalUsers,
        long enabledUsers,
        long disabledUsers,
        long totalRoles,
        long totalPermissions,
        long totalOrganizations,
        long totalSettings
) {
}
