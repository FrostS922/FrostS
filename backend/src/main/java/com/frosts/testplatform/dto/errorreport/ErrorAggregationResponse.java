package com.frosts.testplatform.dto.errorreport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorAggregationResponse {

    private String errorMessage;
    private long count;
    private LocalDateTime lastSeen;
    private LocalDateTime firstSeen;
}
