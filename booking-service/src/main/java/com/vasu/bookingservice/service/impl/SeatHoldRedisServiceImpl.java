package com.vasu.bookingservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vasu.bookingservice.dto.response.UserHoldInfo;
import com.vasu.bookingservice.service.SeatHoldRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatHoldRedisServiceImpl implements SeatHoldRedisService {

    private static final String HOLD_KEY_PREFIX = "hold:";
    private static final String USER_HOLDS_SET_PREFIX = "userHolds:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${booking.seat-hold.ttl:900}")
    private long defaultTtlSeconds;

    /**
     * Internal record to store hold metadata in Redis.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    static class HoldMetadata {
        private String holdReference;
        private Long userId;
        private Long showId;
        private List<Long> seatIds;
        private LocalDateTime createdAt;
    }

    @Override
    public void storeHold(String holdReference, Long userId, Long showId, List<Long> seatIds, long ttlSeconds) {
        String holdKey = HOLD_KEY_PREFIX + holdReference;

        HoldMetadata metadata = HoldMetadata.builder()
                .holdReference(holdReference)
                .userId(userId)
                .showId(showId)
                .seatIds(seatIds)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            String json = objectMapper.writeValueAsString(metadata);
            redisTemplate.opsForValue().set(holdKey, json, ttlSeconds, TimeUnit.SECONDS);

            // Add to user's holds set for quick lookup
            if (userId != null) {
                String userSetKey = USER_HOLDS_SET_PREFIX + userId;
                redisTemplate.opsForSet().add(userSetKey, holdReference);
                redisTemplate.expire(userSetKey, ttlSeconds, TimeUnit.SECONDS);
            }

            log.info("Stored hold {} in Redis with TTL {} seconds", holdReference, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to store hold {} in Redis: {}", holdReference, e.getMessage(), e);
        }
    }

    @Override
    public boolean isHoldActive(String holdReference) {
        String holdKey = HOLD_KEY_PREFIX + holdReference;
        Boolean exists = redisTemplate.hasKey(holdKey);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Long getHoldTtl(String holdReference) {
        String holdKey = HOLD_KEY_PREFIX + holdReference;
        return redisTemplate.getExpire(holdKey, TimeUnit.SECONDS);
    }

    @Override
    public void releaseHold(String holdReference) {
        String holdKey = HOLD_KEY_PREFIX + holdReference;

        // Read metadata to get userId for cleanup
        HoldMetadata metadata = readMetadata(holdKey);
        if (metadata != null && metadata.getUserId() != null) {
            String userSetKey = USER_HOLDS_SET_PREFIX + metadata.getUserId();
            redisTemplate.opsForSet().remove(userSetKey, holdReference);
        }

        redisTemplate.delete(holdKey);
        log.info("Released hold {} from Redis", holdReference);
    }

    @Override
    public List<String> getUserActiveHolds(Long userId) {
        String userSetKey = USER_HOLDS_SET_PREFIX + userId;
        Set<Object> members = redisTemplate.opsForSet().members(userSetKey);

        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter out holds that have expired in Redis
        List<String> activeHolds = new ArrayList<>();
        for (Object member : members) {
            String holdRef = member.toString();
            if (isHoldActive(holdRef)) {
                activeHolds.add(holdRef);
            }
        }
        return activeHolds;
    }

    @Override
    public List<UserHoldInfo> getUserHoldDetails(Long userId) {
        List<String> holdRefs = getUserActiveHolds(userId);
        List<UserHoldInfo> result = new ArrayList<>();

        for (String holdRef : holdRefs) {
            String holdKey = HOLD_KEY_PREFIX + holdRef;
            HoldMetadata metadata = readMetadata(holdKey);
            if (metadata != null) {
                Long ttl = getHoldTtl(holdRef);
                LocalDateTime expiresAt = metadata.getCreatedAt().plusSeconds(ttl != null ? ttl : 0);

                result.add(UserHoldInfo.builder()
                        .holdReference(holdRef)
                        .showId(metadata.getShowId())
                        .seatIds(metadata.getSeatIds())
                        .status("ACTIVE")
                        .ttlSeconds(ttl)
                        .expiresAt(expiresAt)
                        .build());
            }
        }
        return result;
    }

    @Override
    public void releaseAllUserHolds(Long userId) {
        List<String> holdRefs = getUserActiveHolds(userId);
        String userSetKey = USER_HOLDS_SET_PREFIX + userId;

        for (String holdRef : holdRefs) {
            String holdKey = HOLD_KEY_PREFIX + holdRef;
            redisTemplate.delete(holdKey);
        }
        redisTemplate.delete(userSetKey);

        log.info("Released all holds for user {} from Redis ({} holds)", userId, holdRefs.size());
    }

    @Override
    public List<Long> getHoldSeatIds(String holdReference) {
        String holdKey = HOLD_KEY_PREFIX + holdReference;
        HoldMetadata metadata = readMetadata(holdKey);
        return metadata != null ? metadata.getSeatIds() : Collections.emptyList();
    }

    private HoldMetadata readMetadata(String key) {
        try {
            Object json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json.toString(), HoldMetadata.class);
        } catch (Exception e) {
            log.error("Failed to read metadata for key {}: {}", key, e.getMessage(), e);
            return null;
        }
    }
}
