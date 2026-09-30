package com.ctrip.content.service.storage;

import java.io.InputStream;

/**
 * 图片存储服务接口抽象。
 *
 * <p>MVP 阶段使用 {@code LocalStorageServiceImpl}（本地文件系统），
 * 后续可切换 {@code OssStorageServiceImpl}（阿里云 OSS），业务代码无需修改。
 */
public interface ImageStorageService {

    /**
     * 上传图片，返回可访问 URL。
     *
     * @param inputStream      图片输入流
     * @param originalFilename 原始文件名（用于扩展名识别）
     * @return 可访问的图片 URL
     */
    String upload(InputStream inputStream, String originalFilename);

    /**
     * 删除图片。
     *
     * @param url 图片 URL
     */
    void delete(String url);
}
