package com.frosts.testplatform.service.push;

import java.util.Map;

public interface NotificationPushService {

    void pushToUser(Long userId, String type, Map<String, Object> payload);

    void pushToAll(String type, Map<String, Object> payload);
}
