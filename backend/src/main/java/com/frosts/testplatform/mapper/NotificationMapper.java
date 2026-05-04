package com.frosts.testplatform.mapper;

import com.frosts.testplatform.dto.notification.NotificationDetailResponse;
import com.frosts.testplatform.dto.notification.NotificationResponse;
import com.frosts.testplatform.entity.Notification;
import com.frosts.testplatform.entity.NotificationRecipient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "notification.id", target = "id")
    @Mapping(source = "notification.title", target = "title")
    @Mapping(source = "notification.content", target = "content")
    @Mapping(source = "notification.type", target = "type")
    @Mapping(source = "notification.category", target = "category")
    @Mapping(source = "notification.priority", target = "priority")
    @Mapping(source = "notification.targetType", target = "targetType")
    @Mapping(source = "notification.targetId", target = "targetId")
    @Mapping(source = "notification.targetUrl", target = "targetUrl")
    @Mapping(source = "notification.isGlobal", target = "isGlobal")
    @Mapping(source = "isRead", target = "isRead")
    @Mapping(source = "isStarred", target = "isStarred")
    @Mapping(source = "readAt", target = "readAt")
    @Mapping(source = "notification.createdAt", target = "createdAt")
    @Mapping(target = "senderName", expression = "java(getSenderName(recipient))")
    NotificationResponse toResponse(NotificationRecipient recipient);

    @Mapping(source = "notification.id", target = "id")
    @Mapping(source = "notification.title", target = "title")
    @Mapping(source = "notification.content", target = "content")
    @Mapping(source = "notification.type", target = "type")
    @Mapping(source = "notification.category", target = "category")
    @Mapping(source = "notification.priority", target = "priority")
    @Mapping(source = "notification.targetType", target = "targetType")
    @Mapping(source = "notification.targetId", target = "targetId")
    @Mapping(source = "notification.targetUrl", target = "targetUrl")
    @Mapping(source = "notification.isGlobal", target = "isGlobal")
    @Mapping(source = "recipient.isRead", target = "isRead")
    @Mapping(source = "recipient.isStarred", target = "isStarred")
    @Mapping(source = "recipient.readAt", target = "readAt")
    @Mapping(source = "notification.createdAt", target = "createdAt")
    @Mapping(source = "notification.expiresAt", target = "expiresAt")
    @Mapping(target = "senderName", expression = "java(getSenderName(notification))")
    @Mapping(target = "senderId", expression = "java(getSenderId(notification))")
    NotificationDetailResponse toDetailResponse(Notification notification, NotificationRecipient recipient);

    default String getSenderName(NotificationRecipient recipient) {
        return getSenderName(recipient.getNotification());
    }

    default String getSenderName(Notification notification) {
        if (notification.getSender() == null) return "系统";
        return notification.getSender().getRealName() != null
                ? notification.getSender().getRealName()
                : notification.getSender().getUsername();
    }

    default Long getSenderId(Notification notification) {
        return notification.getSender() != null ? notification.getSender().getId() : null;
    }
}
