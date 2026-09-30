package com.ctrip.itinerary.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.common.response.ApiResponse;
import com.ctrip.itinerary.dto.request.*;
import com.ctrip.itinerary.dto.response.*;
import com.ctrip.itinerary.service.ItineraryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 行程控制器。
 *
 * <p>所有端点路径前缀：/api/v1/itineraries
 * 分享查看端点：/api/v1/itineraries/share/{shareCode}（公开）
 */
@RestController
@RequestMapping("/api/v1/itineraries")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }

    // ========== Itinerary CRUD ==========

    @PostMapping
    public ResponseEntity<ApiResponse<ItineraryResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateItineraryRequest request) {
        ItineraryResponse result = itineraryService.createItinerary(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ItineraryListResponse>>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long destinationId) {
        Page<ItineraryListResponse> result = itineraryService.listItineraries(userId, page, limit, status, destinationId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItineraryResponse>> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        ItineraryResponse result = itineraryService.getDetail(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ItineraryResponse>> update(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateItineraryRequest request) {
        ItineraryResponse result = itineraryService.updateItinerary(id, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        itineraryService.deleteItinerary(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ========== Collection ==========

    @PostMapping("/{id}/collection")
    public ResponseEntity<ApiResponse<Void>> toggleCollection(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @RequestParam boolean add) {
        itineraryService.toggleCollection(id, userId, add);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ========== Days ==========

    @PostMapping("/{id}/days")
    public ResponseEntity<ApiResponse<ItineraryResponse>> addDay(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateItineraryDayRequest request) {
        ItineraryResponse result = itineraryService.addDay(id, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/days/{dayId}")
    public ResponseEntity<ApiResponse<ItineraryDayResponse>> updateDay(
            @PathVariable Long dayId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateItineraryDayRequest request) {
        ItineraryDayResponse result = itineraryService.updateDay(dayId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/days/{dayId}")
    public ResponseEntity<ApiResponse<Void>> deleteDay(
            @PathVariable Long dayId,
            @AuthenticationPrincipal Long userId) {
        itineraryService.deleteDay(dayId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ========== Items ==========

    @PostMapping("/{id}/days/{dayId}/items")
    public ResponseEntity<ApiResponse<ItineraryItemResponse>> addItem(
            @PathVariable Long id,
            @PathVariable Long dayId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateItineraryItemRequest request) {
        ItineraryItemResponse result = itineraryService.addItem(id, dayId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<ItineraryItemResponse>> updateItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateItineraryItemRequest request) {
        ItineraryItemResponse result = itineraryService.updateItem(itemId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId) {
        itineraryService.deleteItem(itemId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/days/{dayId}/items/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderItems(
            @PathVariable Long dayId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReorderItemsRequest request) {
        itineraryService.reorderItems(dayId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ========== Share ==========

    /**
     * 通过分享码查看行程（公开访问，无需认证）。
     */
    @GetMapping("/share/{shareCode}")
    public ResponseEntity<ApiResponse<ItineraryResponse>> getSharedItinerary(
            @PathVariable String shareCode) {
        ItineraryResponse result = itineraryService.getSharedItinerary(shareCode);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<ApiResponse<String>> generateShareLink(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        String code = itineraryService.generateShareLink(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(code));
    }

    @DeleteMapping("/{id}/share")
    public ResponseEntity<ApiResponse<Void>> cancelShare(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        itineraryService.cancelShare(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
