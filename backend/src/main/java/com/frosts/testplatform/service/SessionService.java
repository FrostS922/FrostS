package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.SessionInfo;
import com.frosts.testplatform.entity.RefreshToken;
import com.frosts.testplatform.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final RefreshTokenRepository refreshTokenRepository;

    public List<SessionInfo> getUserSessions(String username, Long currentTokenId) {
        List<RefreshToken> tokens = refreshTokenRepository
                .findByUsernameAndIsRevokedFalseAndExpiryDateAfter(username, LocalDateTime.now());

        return tokens.stream().map(token -> SessionInfo.builder()
                .id(token.getId())
                .deviceInfo(token.getDeviceInfo() != null ? token.getDeviceInfo() : "未知设备")
                .clientIp(token.getClientIp() != null ? token.getClientIp() : "未知IP")
                .createdAt(token.getCreatedAt())
                .lastRefreshedAt(token.getLastRefreshedAt() != null ? token.getLastRefreshedAt() : token.getCreatedAt())
                .current(currentTokenId != null && token.getId().equals(currentTokenId))
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public void terminateSession(String username, Long sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        if (!token.getUsername().equals(username)) {
            throw new RuntimeException("无权操作此会话");
        }
        token.setIsRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void terminateAllOtherSessions(String username, Long currentTokenId) {
        List<RefreshToken> tokens = refreshTokenRepository
                .findByUsernameAndIsRevokedFalseAndExpiryDateAfter(username, LocalDateTime.now());
        for (RefreshToken token : tokens) {
            if (!token.getId().equals(currentTokenId)) {
                token.setIsRevoked(true);
            }
        }
        refreshTokenRepository.saveAll(tokens);
    }

    public String parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) return "未知设备";
        String browser = parseBrowser(userAgent);
        String os = parseOS(userAgent);
        return browser + " / " + os;
    }

    private String parseBrowser(String ua) {
        if (ua.contains("Edg/")) return "Edge " + extractVersion(ua, "Edg/");
        if (ua.contains("Chrome/") && !ua.contains("Edg/")) return "Chrome " + extractVersion(ua, "Chrome/");
        if (ua.contains("Firefox/")) return "Firefox " + extractVersion(ua, "Firefox/");
        if (ua.contains("Safari/") && !ua.contains("Chrome/")) return "Safari " + extractVersion(ua, "Version/");
        return "未知浏览器";
    }

    private String parseOS(String ua) {
        if (ua.contains("Windows NT 10.0")) return "Windows 10/11";
        if (ua.contains("Windows NT")) return "Windows";
        if (ua.contains("Mac OS X")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        return "未知系统";
    }

    private String extractVersion(String ua, String prefix) {
        Pattern pattern = Pattern.compile(prefix + "([\\d.]+)");
        Matcher matcher = pattern.matcher(ua);
        if (matcher.find()) {
            String version = matcher.group(1);
            int dotIndex = version.indexOf('.', version.indexOf('.') + 1);
            return dotIndex > 0 ? version.substring(0, dotIndex) : version;
        }
        return "";
    }
}
