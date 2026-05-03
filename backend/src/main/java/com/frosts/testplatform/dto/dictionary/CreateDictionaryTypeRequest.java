package com.frosts.testplatform.dto.dictionary;

public record CreateDictionaryTypeRequest(
    Long parentId,
    String code,
    String name,
    String description,
    Integer sortOrder,
    Boolean enabled
) {}
