package com.frosts.testplatform.dto.dictionary;

import java.util.List;

public record DictionaryTypePermissionRequest(
    List<PermissionItem> permissions
) {
    public record PermissionItem(
        Long roleId,
        String permission
    ) {}
}
