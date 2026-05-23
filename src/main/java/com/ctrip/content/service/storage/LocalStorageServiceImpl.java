package com.ctrip.content.service.storage;

import com.ctrip.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件系统图片存储实现（MVP 阶段）。
 *
 * <p>存储路径：{upload.dir}/images/{yyyy}/{MM}/{dd}/{UUID}.{ext}
 * 按日期分层，避免单目录文件过多。
 */
@Service
public class LocalStorageServiceImpl implements ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    @Value("${app.upload.base-url:/uploads/images}")
    private String baseUrl;

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(Path.of(uploadDir));
    }

    @Override
    public String upload(InputStream inputStream, String originalFilename) {
        // 提取扩展名
        String ext = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("不支持的图片格式：" + ext + "，仅支持 " + ALLOWED_EXTENSIONS);
        }

        // 按日期分层目录
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path dir = Path.of(uploadDir, datePath);

        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException("创建上传目录失败：" + e.getMessage());
        }

        // UUID 文件名
        String filename = UUID.randomUUID() + "." + ext;
        Path target = dir.resolve(filename);

        try {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("保存图片失败：" + e.getMessage());
        }

        // 返回可访问 URL
        return baseUrl + "/" + datePath + "/" + filename;
    }

    @Override
    public void delete(String url) {
        // 从 URL 提取相对路径
        String relativePath = url.replace(baseUrl + "/", "");

        // 防御：校验路径穿越（即使调用方已校验，服务层仍需防御）
        if (relativePath.contains("..")) {
            throw new BusinessException("非法路径");
        }

        Path filePath = Path.of(uploadDir, relativePath).normalize();

        // 确保文件路径在上传目录内
        if (!filePath.startsWith(Path.of(uploadDir).toAbsolutePath().normalize())) {
            throw new BusinessException("非法路径");
        }

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new BusinessException("删除图片失败：" + e.getMessage());
        }
    }

    /**
     * 从文件名提取扩展名（小写，无点）。
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
