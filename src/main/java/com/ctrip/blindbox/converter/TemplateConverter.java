package com.ctrip.blindbox.converter;

import com.ctrip.blindbox.dto.TemplateResponse;
import com.ctrip.blindbox.entity.BlindBoxTemplate;

/**
 * 盲盒模板 Entity ↔ DTO 转换器。
 *
 * 无状态工具类，不注册为 Spring Bean。
 * 枚举统一通过 .name() 转为 String。
 */
public class TemplateConverter {

    private TemplateConverter() {
    }

    /**
     * Entity → Response。
     */
    public static TemplateResponse toResponse(BlindBoxTemplate entity) {
        return new TemplateResponse(
                entity.getId(),
                entity.getName(),
                entity.getType() != null ? entity.getType().name() : null,
                entity.getPrice(),
                entity.getStock(),
                entity.getRuleConfig(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getCreatedAt()
        );
    }
}
