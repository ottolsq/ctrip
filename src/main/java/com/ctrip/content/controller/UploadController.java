package com.ctrip.content.controller;

import com.ctrip.common.exception.AuthenticationException;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.content.dto.response.ImageBatchUploadResponse;
import com.ctrip.content.dto.response.ImageUploadResponse;
import com.ctrip.content.service.storage.ImageStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 图片上传控制器。
 *
 * <p>需 JWT 认证。MVP 阶段使用本地文件存储，后续可切换 OSS。
 */
@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    private final ImageStorageService imageStorageService;

    public UploadController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    /**
     * 上传单张图片。
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> upload(
            @RequestParam("file") MultipartFile file) throws IOException {
        String url = imageStorageService.upload(file.getInputStream(), file.getOriginalFilename());
        return ResponseEntity.ok(ApiResponse.ok(new ImageUploadResponse(url)));
    }

    /**
     * 批量上传图片。
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageBatchUploadResponse>> uploadMultiple(
            @RequestParam("files") MultipartFile[] files) throws IOException {
        java.util.List<String> urls = Arrays.stream(files)
                .map(file -> {
                    try {
                        return imageStorageService.upload(file.getInputStream(), file.getOriginalFilename());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(new ImageBatchUploadResponse(urls)));
    }

    /**
     * 删除已上传的图片（需 JWT 认证）。
     * MVP 阶段暂不校验图片所有者，后续通过 upload_records 表补齐。
     */
    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String filename) {
        validateFilename(filename);
        String url = "/uploads/images/" + filename;
        imageStorageService.delete(url);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * 校验文件名安全：禁止路径穿越（../ 等）。
     */
    private void validateFilename(String filename) {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new AuthenticationException("非法文件名");
        }
    }
}
