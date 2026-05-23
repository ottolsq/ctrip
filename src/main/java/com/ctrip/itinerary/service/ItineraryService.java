package com.ctrip.itinerary.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ctrip.itinerary.dto.request.CreateItineraryDayRequest;
import com.ctrip.itinerary.dto.request.CreateItineraryItemRequest;
import com.ctrip.itinerary.dto.request.CreateItineraryRequest;
import com.ctrip.itinerary.dto.request.ReorderItemsRequest;
import com.ctrip.itinerary.dto.request.UpdateItineraryDayRequest;
import com.ctrip.itinerary.dto.request.UpdateItineraryItemRequest;
import com.ctrip.itinerary.dto.request.UpdateItineraryRequest;
import com.ctrip.itinerary.dto.response.ItineraryDayResponse;
import com.ctrip.itinerary.dto.response.ItineraryItemResponse;
import com.ctrip.itinerary.dto.response.ItineraryListResponse;
import com.ctrip.itinerary.dto.response.ItineraryResponse;

/**
 * 行程服务接口。
 */
public interface ItineraryService {

    // --- Itinerary CRUD ---

    ItineraryResponse createItinerary(Long userId, CreateItineraryRequest request);

    Page<ItineraryListResponse> listItineraries(Long userId, int page, int limit, String status, Long destinationId);

    ItineraryResponse getDetail(Long id, Long userId);

    ItineraryResponse updateItinerary(Long id, Long userId, UpdateItineraryRequest request);

    void deleteItinerary(Long id, Long userId);

    // --- Collection ---

    void toggleCollection(Long id, Long userId, boolean add);

    // --- Days ---

    ItineraryResponse addDay(Long itineraryId, Long userId, CreateItineraryDayRequest request);

    ItineraryDayResponse updateDay(Long dayId, Long userId, UpdateItineraryDayRequest request);

    void deleteDay(Long dayId, Long userId);

    // --- Items ---

    ItineraryItemResponse addItem(Long itineraryId, Long dayId, Long userId, CreateItineraryItemRequest request);

    ItineraryItemResponse updateItem(Long itemId, Long userId, UpdateItineraryItemRequest request);

    void deleteItem(Long itemId, Long userId);

    void reorderItems(Long dayId, Long userId, ReorderItemsRequest request);

    // --- Share ---

    String generateShareLink(Long id, Long userId);

    ItineraryResponse getSharedItinerary(String shareCode);

    void cancelShare(Long id, Long userId);
}
