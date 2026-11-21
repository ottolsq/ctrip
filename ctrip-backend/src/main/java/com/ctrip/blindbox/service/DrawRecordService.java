package com.ctrip.blindbox.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.DrawRecordResponse;
import com.ctrip.blindbox.entity.BlindBoxOrder;
import com.ctrip.blindbox.prize.DrawnPrize;
import com.ctrip.blindbox.prize.PrizeTier;

import java.time.LocalDate;

/**
 * 盲盒抽取记录服务。
 */
public interface DrawRecordService {

    void record(BlindBoxOrder order, DrawnPrize prize);

    Page<DrawRecordResponse> listMine(Long userId, int page, int size);

    Page<DrawRecordResponse> listAll(int page, int size, Long userId, Long templateId,
                                     PrizeTier tier, LocalDate startDate, LocalDate endDate);
}
