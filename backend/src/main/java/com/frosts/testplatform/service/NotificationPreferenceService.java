package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.notification.NotificationPreferenceResponse;
import com.frosts.testplatform.dto.notification.UpdateNotificationPreferenceRequest;
import com.frosts.testplatform.entity.NotificationPreference;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.repository.NotificationPreferenceRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreference(Long userId) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
        return toResponse(preference);
    }

    public NotificationPreferenceResponse updatePreference(Long userId, UpdateNotificationPreferenceRequest request) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));

        if (request.getTypeSettings() != null) {
            preference.setTypeSettings(request.getTypeSettings());
        }
        if (request.getCategorySettings() != null) {
            preference.setCategorySettings(request.getCategorySettings());
        }
        if (request.getReceiveChannels() != null) {
            preference.setReceiveChannels(request.getReceiveChannels());
        }
        if (request.getQuietHoursStart() != null) {
            preference.setQuietHoursStart(request.getQuietHoursStart());
        }
        if (request.getQuietHoursEnd() != null) {
            preference.setQuietHoursEnd(request.getQuietHoursEnd());
        }

        preference = preferenceRepository.save(preference);
        return toResponse(preference);
    }

    private NotificationPreference createDefaultPreference(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));

        Map<String, Boolean> defaultTypeSettings = new HashMap<>();
        defaultTypeSettings.put("SYSTEM", true);
        defaultTypeSettings.put("BUSINESS", true);
        defaultTypeSettings.put("REMINDER", true);
        defaultTypeSettings.put("TODO", true);

        Map<String, Boolean> defaultCategorySettings = new HashMap<>();

        Map<String, Boolean> defaultChannels = new HashMap<>();
        defaultChannels.put("in_app", true);
        defaultChannels.put("email", false);

        NotificationPreference preference = new NotificationPreference();
        preference.setUser(user);
        preference.setTypeSettings(defaultTypeSettings);
        preference.setCategorySettings(defaultCategorySettings);
        preference.setReceiveChannels(defaultChannels);

        return preferenceRepository.save(preference);
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .id(preference.getId())
                .userId(preference.getUser().getId())
                .typeSettings(preference.getTypeSettings())
                .categorySettings(preference.getCategorySettings())
                .receiveChannels(preference.getReceiveChannels())
                .quietHoursStart(preference.getQuietHoursStart())
                .quietHoursEnd(preference.getQuietHoursEnd())
                .build();
    }
}
