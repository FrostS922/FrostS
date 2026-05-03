package com.frosts.testplatform.dto.dictionary;

public record UpdateDictionaryItemRequest(
    String code,
    String name,
    String value,
    String description,
    Integer sortOrder,
    Boolean enabled,
    Boolean isDefault,
    String color
) {}
