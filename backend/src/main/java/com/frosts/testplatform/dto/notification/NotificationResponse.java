package com.frosts.testplatform.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String title;
    private String content;
    private String type;
    private String category;
    private String priority;
    private String targetType;
    private Long targetId;
    private String targetUrl;
    private Boolean isGlobal;
    private Boolean isRead;
    private Boolean isStarred;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private String senderName;
}
