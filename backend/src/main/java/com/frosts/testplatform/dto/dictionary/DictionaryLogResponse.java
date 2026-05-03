package com.frosts.testplatform.dto.dictionary;

import java.time.LocalDateTime;

public record DictionaryLogResponse(
    Long id,
    Long typeId,
    String typeCode,
    Long itemId,
    String itemCode,
    String action,
    String oldValue,
    String newValue,
    String operator,
    LocalDateTime operatedAt,
    String ipAddress
) {}
