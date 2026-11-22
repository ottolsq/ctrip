package com.ctrip.blindbox.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.blindbox.dto.DrawRecordResponse;
import com.ctrip.blindbox.dto.OrderResponse;
import com.ctrip.blindbox.dto.ProbabilityResponse;
import com.ctrip.blindbox.dto.TemplateResponse;
import com.ctrip.blindbox.dto.ResultResponse;
import com.ctrip.blindbox.prize.BlindBoxPrizeService;
import com.ctrip.blindbox.service.BlindBoxItineraryImportService;
import com.ctrip.blindbox.service.BlindBoxOrderService;
import com.ctrip.blindbox.service.BlindBoxResultService;
import com.ctrip.blindbox.service.BlindBoxTemplateService;
import com.ctrip.blindbox.service.DrawRecordService;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.itinerary.dto.response.ItineraryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 盲盒前台控制器。
 *
 * <p>路径前缀：/api/v1/blind-box
 * 盲盒模板列表/详情为公开接口（无需认证），分享查看也为公开。
 */
@RestController
@RequestMapping("/api/v1/blind-box")
public class BlindBoxController {

    private final BlindBoxTemplateService templateService;
    private final BlindBoxResultService resultService;
    private final BlindBoxOrderService orderService;
    private final BlindBoxPrizeService prizeService;
    private final DrawRecordService drawRecordService;
    private final BlindBoxItineraryImportService itineraryImportService;

    public BlindBoxController(BlindBoxTemplateService templateService,
                              BlindBoxResultService resultService,
                              BlindBoxOrderService orderService,
                              BlindBoxPrizeService prizeService,
                              DrawRecordService drawRecordService,
                              BlindBoxItineraryImportService itineraryImportService) {
        this.templateService = templateService;
        this.resultService = resultService;
        this.orderService = orderService;
        this.prizeService = prizeService;
        this.drawRecordService = drawRecordService;
        this.itineraryImportService = itineraryImportService;
    }

    /**
     * 盲盒模板列表（分页，公开）。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TemplateResponse>>> listTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String type) {
        Page<TemplateResponse> result = templateService.listActiveTemplates(page, limit, type);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 模板详情（公开）。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplate(@PathVariable Long id) {
        TemplateResponse result = templateService.getTemplateDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 概率公示（公开）。
     */
    @GetMapping("/{id}/probabilities")
    public ResponseEntity<ApiResponse<ProbabilityResponse>> getProbabilities(@PathVariable Long id) {
        // 仅上架模板可查看概率
        templateService.getTemplateDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(prizeService.getProbabilities()));
    }

    /**
     * 将开盒结果导入为个人行程（原子、幂等）。
     */
    @PostMapping("/orders/{orderNo}/import-itinerary")
    public ResponseEntity<ApiResponse<ItineraryResponse>> importItinerary(
            @AuthenticationPrincipal Long userId,
            @PathVariable String orderNo) {
        return ResponseEntity.ok(ApiResponse.ok(
                itineraryImportService.importItinerary(userId, orderNo)));
    }

    /**
     * 我的抽取记录（需登录）。
     */
    @GetMapping("/draw-records")
    public ResponseEntity<ApiResponse<Page<DrawRecordResponse>>> listMyDrawRecords(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                drawRecordService.listMine(userId, page, size)));
    }

    /**
     * 我的盲盒列表（含开盒状态，分页）。
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> listMyBlindBoxes(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Boolean opened) {
        Page<OrderResponse> result = orderService.listMyBlindBoxes(userId, page, limit, opened);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 查看分享结果（公开，无需认证）。
     */
    @GetMapping("/share/{shareCode}")
    public ResponseEntity<ApiResponse<ResultResponse>> getSharedResult(@PathVariable String shareCode) {
        ResultResponse result = resultService.getSharedResult(shareCode);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
