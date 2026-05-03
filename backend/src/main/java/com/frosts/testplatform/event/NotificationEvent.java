package com.frosts.testplatform.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class NotificationEvent extends ApplicationEvent {

    private final String type;
    private final String category;
    private final String title;
    private final String content;
    private final String priority;
    private final List<Long> recipientIds;
    private final String targetType;
    private final Long targetId;
    private final String targetUrl;
    private final Long senderId;
    private final boolean isGlobal;

    public NotificationEvent(Object source, String type, String category,
                             String title, String content, String priority,
                             List<Long> recipientIds, String targetType,
                             Long targetId, String targetUrl, Long senderId,
                             boolean isGlobal) {
        super(source);
        this.type = type;
        this.category = category;
        this.title = title;
        this.content = content;
        this.priority = priority;
        this.recipientIds = recipientIds;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetUrl = targetUrl;
        this.senderId = senderId;
        this.isGlobal = isGlobal;
    }

    public static NotificationEventBuilder builder() {
        return new NotificationEventBuilder();
    }

    public static class NotificationEventBuilder {
        private Object source;
        private String type;
        private String category;
        private String title;
        private String content;
        private String priority = "NORMAL";
        private List<Long> recipientIds;
        private String targetType;
        private Long targetId;
        private String targetUrl;
        private Long senderId;
        private boolean isGlobal = false;

        public NotificationEventBuilder source(Object source) { this.source = source; return this; }
        public NotificationEventBuilder type(String type) { this.type = type; return this; }
        public NotificationEventBuilder category(String category) { this.category = category; return this; }
        public NotificationEventBuilder title(String title) { this.title = title; return this; }
        public NotificationEventBuilder content(String content) { this.content = content; return this; }
        public NotificationEventBuilder priority(String priority) { this.priority = priority; return this; }
        public NotificationEventBuilder recipientIds(List<Long> recipientIds) { this.recipientIds = recipientIds; return this; }
        public NotificationEventBuilder targetType(String targetType) { this.targetType = targetType; return this; }
        public NotificationEventBuilder targetId(Long targetId) { this.targetId = targetId; return this; }
        public NotificationEventBuilder targetUrl(String targetUrl) { this.targetUrl = targetUrl; return this; }
        public NotificationEventBuilder senderId(Long senderId) { this.senderId = senderId; return this; }
        public NotificationEventBuilder isGlobal(boolean isGlobal) { this.isGlobal = isGlobal; return this; }

        public NotificationEvent build() {
            return new NotificationEvent(source, type, category, title, content,
                    priority, recipientIds, targetType, targetId, targetUrl, senderId, isGlobal);
        }
    }
}
