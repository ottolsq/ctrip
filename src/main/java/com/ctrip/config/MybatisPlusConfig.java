package com.ctrip.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 全局配置。
 *
 * <p>目前包含：
 * <ul>
 *   <li>{@link MetaObjectHandler} 实现 — 在 INSERT/UPDATE 时自动填充审计时间字段，
 *       避免每次手动赋值 createdAt / updatedAt / issuedAt。
 * </ul>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 自动填充处理器。
     *
     * <p>只有实体字段上标注了对应的 {@code @TableField(fill = FieldFill.INSERT)} 或
     * {@code @TableField(fill = FieldFill.INSERT_UPDATE)} 时，此处理器才会生效。
     * 若实体不含对应字段，{@code strictInsertFill} 会静默跳过，不会报错。
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {

            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                // User.createdAt、User.updatedAt、RefreshToken.issuedAt
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "issuedAt",  LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                // 仅 User.updatedAt 标注了 INSERT_UPDATE，其他实体不受影响
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
