package com.vasu.bookingservice.service;

import com.vasu.bookingservice.dto.response.UserHoldInfo;

import java.util.List;

public interface SeatHoldRedisService {

    /**
     * Store a hold reference in Redis with TTL for automatic expiry.
     */
    void storeHold(String holdReference, Long userId, Long showId, List<Long> seatIds, long ttlSeconds);

    /**
     * Check if a hold is still active in Redis.
     */
    boolean isHoldActive(String holdReference);

    /**
     * Get the remaining TTL for a hold in seconds.
     */
    Long getHoldTtl(String holdReference);

    /**
     * Remove a hold from Redis (manual release).
     */
    void releaseHold(String holdReference);

    /**
     * Get all active hold references for a user.
     */
    List<String> getUserActiveHolds(Long userId);

    /**
     * Get detailed hold information for a user.
     */
    List<UserHoldInfo> getUserHoldDetails(Long userId);

    /**
     * Release all active holds for a user.
     */
    void releaseAllUserHolds(Long userId);

    /**
     * Get the seat IDs associated with a hold.
     */
    List<Long> getHoldSeatIds(String holdReference);
}
