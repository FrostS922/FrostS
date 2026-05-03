package com.frosts.testplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frosts.testplatform.dto.MfaSetupResponse;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.repository.UserRepository;
import com.frosts.testplatform.security.JwtTokenProvider;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${app.mfa.issuer}")
    private String issuer;

    @Value("${app.mfa.encryption-key}")
    private String encryptionKey;

    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public MfaSetupResponse setupMfa(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new RuntimeException("MFA已启用，请先禁用后再重新设置");
        }

        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        String secret = secretGenerator.generate();

        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrCodeBase64;
        try {
            ZxingPngQrGenerator qrGenerator = new ZxingPngQrGenerator();
            byte[] qrImageData = qrGenerator.generate(data);
            qrCodeBase64 = Base64.getEncoder().encodeToString(qrImageData);
        } catch (QrGenerationException e) {
            throw new RuntimeException("QR码生成失败", e);
        }

        List<String> backupCodes = generateBackupCodes();

        user.setMfaSecret(encryptSecret(secret));
        user.setMfaBackupCodes(hashBackupCodes(backupCodes));
        userRepository.save(user);

        return MfaSetupResponse.builder()
                .secret(secret)
                .otpAuthUrl(data.getUri())
                .qrCodeBase64(qrCodeBase64)
                .backupCodes(backupCodes)
                .build();
    }

    @Transactional
    public void verifySetup(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        String secret = decryptSecret(user.getMfaSecret());
        if (!verifyTotpCode(secret, code)) {
            throw new RuntimeException("验证码不正确");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
        log.info("[MFA] 用户 {} 已启用MFA", username);
    }

    public boolean verifyCode(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            return true;
        }

        String secret = decryptSecret(user.getMfaSecret());
        if (verifyTotpCode(secret, code)) {
            return true;
        }

        return useBackupCode(user, code);
    }

    public String generateMfaToken(String username) {
        return jwtTokenProvider.generateMfaToken(username);
    }

    public boolean validateMfaToken(String mfaToken) {
        return jwtTokenProvider.validateMfaToken(mfaToken);
    }

    public String getUsernameFromMfaToken(String mfaToken) {
        return jwtTokenProvider.getUsernameFromMfaToken(mfaToken);
    }

    @Transactional
    public void disableMfa(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码不正确");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaBackupCodes(null);
        userRepository.save(user);
        log.info("[MFA] 用户 {} 已禁用MFA", username);
    }

    public boolean isMfaEnabled(String username) {
        return userRepository.findByUsername(username)
                .map(user -> Boolean.TRUE.equals(user.getMfaEnabled()))
                .orElse(false);
    }

    private boolean verifyTotpCode(String secret, String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(secret, code);
    }

    private boolean useBackupCode(User user, String code) {
        try {
            List<String> hashedCodes = objectMapper.readValue(
                    user.getMfaBackupCodes(), new TypeReference<List<String>>() {});
            for (int i = 0; i < hashedCodes.size(); i++) {
                if (passwordEncoder.matches(code, hashedCodes.get(i))) {
                    hashedCodes.remove(i);
                    user.setMfaBackupCodes(objectMapper.writeValueAsString(hashedCodes));
                    userRepository.save(user);
                    log.info("[MFA] 用户 {} 使用了备份码，剩余 {} 个", user.getUsername(), hashedCodes.size());
                    return true;
                }
            }
        } catch (JsonProcessingException e) {
            log.error("[MFA] 解析备份码失败", e);
        }
        return false;
    }

    private List<String> generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder sb = new StringBuilder(BACKUP_CODE_LENGTH);
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                sb.append("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(random.nextInt(36)));
            }
            codes.add(sb.toString());
        }
        return codes;
    }

    private String hashBackupCodes(List<String> codes) {
        try {
            List<String> hashed = codes.stream()
                    .map(passwordEncoder::encode)
                    .collect(Collectors.toList());
            return objectMapper.writeValueAsString(hashed);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("备份码序列化失败", e);
        }
    }

    private String encryptSecret(String plaintext) {
        try {
            byte[] keyBytes = normalizeKey(encryptionKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    private String decryptSecret(String encrypted) {
        try {
            byte[] keyBytes = normalizeKey(encryptionKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            byte[] combined = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    private byte[] normalizeKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] normalized = new byte[32];
        System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, 32));
        return normalized;
    }
}
