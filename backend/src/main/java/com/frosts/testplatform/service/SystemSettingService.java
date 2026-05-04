package com.frosts.testplatform.service;

import com.frosts.testplatform.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public boolean isOpenRegistration() {
        return systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse("security.open_registration")
                .map(setting -> "true".equalsIgnoreCase(setting.getSettingValue()))
                .orElse(false);
    }

    public Map<String, Object> getPasswordPolicy() {
        int minLength = systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse("security.password_min_length")
                .map(setting -> {
                    try {
                        return Integer.parseInt(setting.getSettingValue());
                    } catch (NumberFormatException e) {
                        return 6;
                    }
                })
                .orElse(6);

        String complexity = systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse("security.password_complexity")
                .map(setting -> setting.getSettingValue() != null ? setting.getSettingValue() : "LOW")
                .orElse("LOW");

        return Map.of("minLength", minLength, "complexity", complexity);
    }

    public String getSettingValue(String key) {
        return systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse(key)
                .map(setting -> setting.getSettingValue())
                .orElse(null);
    }

    public int getSettingAsInt(String key, int defaultValue) {
        return systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse(key)
                .map(setting -> {
                    try {
                        return Integer.parseInt(setting.getSettingValue());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }
}
