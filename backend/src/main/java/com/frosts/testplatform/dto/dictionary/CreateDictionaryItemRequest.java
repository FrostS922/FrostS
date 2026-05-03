package com.frosts.testplatform.dto.dictionary;

public record CreateDictionaryItemRequest(
    Long typeId,
    String code,
    String name,
    String value,
    String description,
    Integer sortOrder,
    Boolean enabled,
    Boolean isDefault,
    String color
) {}
