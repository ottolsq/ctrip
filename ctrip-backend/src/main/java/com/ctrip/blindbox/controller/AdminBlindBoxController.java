package com.ctrip.blindbox.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.TemplateResponse;
import com.ctrip.blindbox.entity.BlindBoxTemplate;
import com.ctrip.blindbox.entity.enums.BlindBoxType;
import com.ctrip.blindbox.entity.enums.TemplateStatus;
import com.ctrip.blindbox.service.BlindBoxTemplateService;
import com.ctrip.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 后台盲盒管理控制器。
 *
 * <p>路径前缀：/api/v1/admin/blind-box
 * 所有端点需要 JWT + ADMIN 角色。
 */
@RestController
@RequestMapping("/api/v1/admin/blind-box")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlindBoxController {

    private final BlindBoxTemplateService templateService;

    public AdminBlindBoxController(BlindBoxTemplateService templateService) {
        this.templateService = templateService;
    }

    // ========== 模板管理 ==========

    /**
     * 模板列表（分页）。
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<Page<TemplateResponse>>> listTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) BlindBoxType type,
            @RequestParam(required = false) TemplateStatus status,
            @RequestParam(required = false) String keyword) {
        Page<TemplateResponse> result = templateService.listAllTemplates(page, limit, type, status, keyword);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 创建模板。
     */
    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @RequestBody BlindBoxTemplate template) {
        TemplateResponse result = templateService.createTemplate(template);
        return ResponseEntity.status(201).body(ApiResponse.ok(result));
    }

    /**
     * 更新模板。
     */
    @PutMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @RequestBody BlindBoxTemplate template) {
        TemplateResponse result = templateService.updateTemplate(id, template);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 删除模板。
     */
    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
