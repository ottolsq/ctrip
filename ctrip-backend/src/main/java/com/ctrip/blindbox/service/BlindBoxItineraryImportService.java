package com.ctrip.blindbox.service;

import com.ctrip.itinerary.dto.response.ItineraryResponse;

/**
 * 盲盒结果导入个人行程。
 */
public interface BlindBoxItineraryImportService {

    /**
     * 将已开盒结果原子、幂等地导入为个人行程。
     */
    ItineraryResponse importItinerary(Long userId, String orderNo);
}
