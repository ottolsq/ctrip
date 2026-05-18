package com.ctrip.content.controller;

import com.ctrip.common.response.ApiResponse;
import com.ctrip.content.dto.response.ImageUploadResponse;
import com.ctrip.content.service.storage.ImageStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

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
    public ResponseEntity<ApiResponse<ImageUploadResponse[]>> uploadMultiple(
            @RequestParam("files") MultipartFile[] files) throws IOException {
        ImageUploadResponse[] responses = Arrays.stream(files)
                .map(file -> {
                    try {
                        String url = imageStorageService.upload(file.getInputStream(), file.getOriginalFilename());
                        return new ImageUploadResponse(url);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(ImageUploadResponse[]::new);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    /**
     * 删除图片。
     */
    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String filename) {
        // 构建完整 URL 路径
        String url = "/uploads/images/" + filename;
        imageStorageService.delete(url);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
