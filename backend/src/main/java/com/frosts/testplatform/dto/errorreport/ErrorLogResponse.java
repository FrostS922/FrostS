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
public class ErrorLogResponse {

    private Long id;
    private String errorMessage;
    private String stackTrace;
    private String pageUrl;
    private String userAgent;
    private Integer httpStatus;
    private String fallbackMessage;
    private String extraInfo;
    private String category;
    private LocalDateTime createdAt;
}
