package com.frosts.testplatform.dto.dictionary;

import java.time.LocalDateTime;
import java.util.List;

public record DictionaryTypeResponse(
    Long id,
    Long parentId,
    String code,
    String name,
    String description,
    Integer sortOrder,
    Boolean enabled,
    Boolean isSystem,
    List<DictionaryTypeResponse> children,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
