package com.frosts.testplatform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.file.avatar-dir:./uploads/avatars}")
    private String avatarDir;

    @Value("${app.file.sourcemap-dir:./uploads/sourcemaps}")
    private String sourcemapDir;

    public String storeAvatar(MultipartFile file) {
        validateImageFile(file);
        try {
            Path dirPath = Paths.get(avatarDir).toAbsolutePath().normalize();
            Files.createDirectories(dirPath);

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            }
            if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
                throw new RuntimeException("仅支持 JPG/PNG/GIF/WEBP 格式的图片");
            }

            String filename = UUID.randomUUID().toString() + extension;
            Path targetPath = dirPath.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("[FILE_UPLOAD] 头像上传成功: {}", filename);
            return "/files/avatars/" + filename;
        } catch (IOException e) {
            log.error("[FILE_UPLOAD] 文件存储失败", e);
            throw new RuntimeException("文件存储失败: " + e.getMessage());
        }
    }

    public Path getAvatarPath(String filename) {
        Path filePath = Paths.get(avatarDir).toAbsolutePath().normalize().resolve(filename);
        if (!Files.exists(filePath)) {
            return null;
        }
        return filePath;
    }

    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith("/files/avatars/")) {
            return;
        }
        String filename = avatarUrl.substring("/files/avatars/".length());
        try {
            Path filePath = Paths.get(avatarDir).toAbsolutePath().normalize().resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("[FILE_DELETE] 删除头像文件失败: {}", filename, e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("仅支持上传图片文件");
        }
    }

    public String storeSourceMap(MultipartFile file, String version) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new RuntimeException("Source Map 文件大小不能超过 50MB");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.endsWith(".map")) {
            throw new RuntimeException("仅支持 .map 格式的 Source Map 文件");
        }
        try {
            Path versionDir = Paths.get(sourcemapDir).toAbsolutePath().normalize().resolve(version);
            Files.createDirectories(versionDir);
            Path targetPath = versionDir.resolve(originalName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("[SOURCEMAP_UPLOAD] Source Map 上传成功: version={}, file={}", version, originalName);
            return "/files/sourcemaps/" + version + "/" + originalName;
        } catch (IOException e) {
            log.error("[SOURCEMAP_UPLOAD] Source Map 存储失败", e);
            throw new RuntimeException("Source Map 存储失败: " + e.getMessage());
        }
    }

    public Path getSourceMapPath(String version, String filename) {
        Path filePath = Paths.get(sourcemapDir).toAbsolutePath().normalize().resolve(version).resolve(filename);
        if (!Files.exists(filePath)) {
            return null;
        }
        return filePath;
    }
}
