package com.frosts.testplatform.dto.dictionary;

import java.time.LocalDateTime;

public record DictionaryItemResponse(
    Long id,
    Long typeId,
    String typeCode,
    String typeName,
    String code,
    String name,
    String value,
    String description,
    Integer sortOrder,
    Boolean enabled,
    Boolean isDefault,
    String color,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
