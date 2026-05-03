package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final StringRedisTemplate redisTemplate;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CAPTCHA_LENGTH = 4;
    private static final int CAPTCHA_WIDTH = 120;
    private static final int CAPTCHA_HEIGHT = 40;

    public CaptchaResponse generateCaptcha() {
        String code = generateCode();
        String key = java.util.UUID.randomUUID().toString().replace("-", "");

        redisTemplate.opsForValue().set(CAPTCHA_PREFIX + key, code, 5, TimeUnit.MINUTES);

        BufferedImage image = createCaptchaImage(code);
        String base64Image = "data:image/png;base64," + encodeImage(image);

        return new CaptchaResponse(key, base64Image);
    }

    public boolean validateCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }
        String redisKey = CAPTCHA_PREFIX + captchaKey;
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        if (storedCode != null) {
            redisTemplate.delete(redisKey);
        }
        return storedCode != null && storedCode.equalsIgnoreCase(captchaCode.trim());
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(CAPTCHA_CHARS.charAt(RANDOM.nextInt(CAPTCHA_CHARS.length())));
        }
        return sb.toString();
    }

    private BufferedImage createCaptchaImage(String code) {
        BufferedImage image = new BufferedImage(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, CAPTCHA_WIDTH, CAPTCHA_HEIGHT);

        g.setColor(new Color(200, 200, 200));
        for (int i = 0; i < 6; i++) {
            int x1 = RANDOM.nextInt(CAPTCHA_WIDTH);
            int y1 = RANDOM.nextInt(CAPTCHA_HEIGHT);
            int x2 = RANDOM.nextInt(CAPTCHA_WIDTH);
            int y2 = RANDOM.nextInt(CAPTCHA_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        for (int i = 0; i < 30; i++) {
            int x = RANDOM.nextInt(CAPTCHA_WIDTH);
            int y = RANDOM.nextInt(CAPTCHA_HEIGHT);
            g.setColor(new Color(RANDOM.nextInt(200), RANDOM.nextInt(200), RANDOM.nextInt(200)));
            g.fillOval(x, y, 2, 2);
        }

        g.setFont(new Font("Arial", Font.BOLD, 28));
        int charWidth = (CAPTCHA_WIDTH - 20) / CAPTCHA_LENGTH;
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(RANDOM.nextInt(100), RANDOM.nextInt(100), RANDOM.nextInt(100)));
            int x = 10 + i * charWidth + RANDOM.nextInt(5);
            int y = 28 + RANDOM.nextInt(8) - 4;
            g.drawString(String.valueOf(code.charAt(i)), x, y);
        }

        g.dispose();
        return image;
    }

    private String encodeImage(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("验证码图片编码失败", e);
        }
    }
}
