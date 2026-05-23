package com.ctrip.content.dto.response;

import java.util.List;

/**
 * 批量图片上传响应 DTO。
 */
public record ImageBatchUploadResponse(
        List<String> urls
) {}
