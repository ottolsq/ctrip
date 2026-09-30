package com.ctrip.content.service.storage;

import com.ctrip.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件系统图片存储实现（MVP 阶段）。
 *
 * <p>存储路径：{upload.dir}/{UUID}.{ext}
 * 访问 URL：{base-url}/{UUID}.{ext}
 *
 * <p>支持格式：jpg、jpeg、png、webp。
 * 单文件最大 5MB，上传时校验文件头魔数防止类型伪造。
 */
@Service
public class LocalStorageServiceImpl implements ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    @Value("${app.upload.base-url:/uploads/images}")
    private String baseUrl;

    // ===== Magic Bytes 校验 =====

    private static final byte[][] JPEG_MAGIC = {
        {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}            // FF D8 FF
    };
    private static final byte[] PNG_MAGIC = {
        (byte) 0x89, 0x50, 0x4E, 0x47                       // 89 50 4E 47
    };
    private static final byte[][] WEBP_MAGIC = {
        {0x52, 0x49, 0x46, 0x46}                            // RIFF
    };

    /**
     * 校验文件头魔数与扩展名是否匹配。
     * 防止攻击者修改扩展名绕过格式校验。
     */
    private boolean isValidMagicBytes(byte[] header, String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> matchesAny(header, JPEG_MAGIC);
            case "png" -> matchesExact(header, PNG_MAGIC);
            case "webp" -> matchesAny(header, WEBP_MAGIC);
            default -> false;
        };
    }

    private boolean matchesAny(byte[] header, byte[][] magics) {
        for (byte[] magic : magics) {
            if (matchesExact(header, magic)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesExact(byte[] header, byte[] magic) {
        if (header.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String upload(InputStream inputStream, String originalFilename) {
        // ① 提取扩展名 → 校验白名单
        String ext = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("不支持的图片格式：" + ext + "，仅支持 " + ALLOWED_EXTENSIONS);
        }

        try {
            // ② 读取文件头 → 校验魔数，同时复制到内存缓冲
            byte[] buffer = inputStream.readAllBytes();

            // ③ 文件大小冗余校验（多层防御）
            if (buffer.length > MAX_FILE_SIZE) {
                throw new BusinessException("图片大小不能超过 " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
            }

            // ④ 校验文件头魔数与扩展名匹配
            byte[] header = new byte[Math.min(8, buffer.length)];
            System.arraycopy(buffer, 0, header, 0, header.length);
            if (!isValidMagicBytes(header, ext)) {
                throw new BusinessException("文件类型与扩展名不匹配，拒绝上传");
            }

            // ⑤ 写入磁盘
            String filename = UUID.randomUUID() + "." + ext;
            Path target = Path.of(uploadDir, filename);

            Files.createDirectories(Path.of(uploadDir));
            Files.write(target, buffer);

            // ⑥ 返回可访问 URL
            return baseUrl + "/" + filename;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("保存图片失败：" + e.getMessage());
        }
    }

    @Override
    public void delete(String url) {
        // 从 URL 提取文件名（格式：/uploads/images/{filename}.{ext}）
        String relativePath = url.startsWith(baseUrl + "/")
                ? url.substring(baseUrl.length() + 1)
                : url;

        // 防御：校验路径穿越
        if (relativePath.contains("..") || relativePath.contains("/") || relativePath.contains("\\")) {
            throw new BusinessException("非法路径：文件名包含非法字符");
        }

        Path uploadDirPath = Path.of(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadDirPath.resolve(relativePath).normalize();

        // 确保文件路径在上传目录内
        if (!filePath.startsWith(uploadDirPath)) {
            throw new BusinessException("非法路径：文件不在上传目录内");
        }

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (!deleted) {
                throw new BusinessException("图片文件不存在");
            }
        } catch (BusinessException e) {
            throw e;
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
