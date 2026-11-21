package com.ctrip.blindbox.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ctrip.blindbox.prize.PrizeTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 盲盒抽取记录，映射 blind_box_draw_record 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("blind_box_draw_record")
public class BlindBoxDrawRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long userId;

    private Long templateId;

    private PrizeTier tier;

    private Long destinationId;

    private String destinationName;

    private Boolean guaranteed;

    private String probabilityVersion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
