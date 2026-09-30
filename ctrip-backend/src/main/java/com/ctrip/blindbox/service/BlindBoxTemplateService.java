package com.ctrip.blindbox.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.TemplateResponse;

/**
 * 盲盒模板服务接口。
 *
 * 负责模板的查询操作：列表展示、详情查看。
 */
public interface BlindBoxTemplateService {

    /**
     * 查询上架中的盲盒模板列表（分页）。
     *
     * @param page 页码（从 1 开始）
     * @param limit 每页条数
     * @param type 筛选类型：DAILY / LIMITED（可选）
     * @return 分页的模板列表
     */
    Page<TemplateResponse> listActiveTemplates(int page, int limit, String type);

    /**
     * 获取模板详情。
     *
     * @param id 模板ID
     * @return 模板详情
     */
    TemplateResponse getTemplateDetail(Long id);
}
