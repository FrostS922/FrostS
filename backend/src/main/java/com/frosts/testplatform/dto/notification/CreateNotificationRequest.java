package com.frosts.testplatform.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200个字符")
    private String title;

    private String content;

    @NotBlank(message = "类型不能为空")
    private String type;

    private String category;

    private String priority;

    private Long senderId;

    private List<Long> recipientIds;

    private String targetType;

    private Long targetId;

    private String targetUrl;

    private Boolean isGlobal;

    private LocalDateTime expiresAt;
}
