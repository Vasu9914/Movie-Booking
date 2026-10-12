package com.vasu.bookingservice.scheduler;

import com.vasu.bookingservice.entity.SeatHold;
import com.vasu.bookingservice.enums.HoldStatus;
import com.vasu.bookingservice.repository.SeatHoldRepository;
import com.vasu.bookingservice.repository.ShowSeatRepository;
import com.vasu.bookingservice.service.SeatHoldRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled task that periodically checks for seat holds where the Redis key has expired
 * and releases those seats back to AVAILABLE status.
 * <p>
 * This prevents seats from being stuck in HELD status indefinitely when a user
 * holds seats but walks away without completing the booking.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeatHoldCleanupScheduler {

    private final SeatHoldRepository seatHoldRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatHoldRedisService seatHoldRedisService;

    @Value("${booking.seat-hold.cleanup-interval:120000}")
    private long cleanupIntervalMs;

    /**
     * Runs every 2 minutes by default to clean up expired holds.
     * Checks all ACTIVE holds in the database and verifies if the corresponding
     * Redis key still exists. If Redis key is gone (TTL expired), the hold is
     * marked as EXPIRED and seats are released back to AVAILABLE.
     */
    @Scheduled(fixedRateString = "${booking.seat-hold.cleanup-interval:120000}")
    @Transactional
    public void cleanupExpiredHolds() {
        log.debug("Starting scheduled cleanup of expired seat holds");

        List<SeatHold> activeHolds = seatHoldRepository.findByStatus(HoldStatus.ACTIVE);
        if (activeHolds.isEmpty()) {
            log.debug("No active holds found, skipping cleanup");
            return;
        }

        int releasedCount = 0;
        for (SeatHold hold : activeHolds) {
            if (!seatHoldRedisService.isHoldActive(hold.getHoldReference())) {
                // Redis TTL has expired - mark DB hold as expired and release seats
                seatHoldRepository.updateStatusByHoldReference(hold.getHoldReference(), HoldStatus.EXPIRED);

                List<Long> seatIds = hold.getShowSeatIds();
                if (!seatIds.isEmpty()) {
                    int released = showSeatRepository.releaseSeats(seatIds);
                    log.info("Released {} seats for expired hold {} (Redis TTL expired)", released, hold.getHoldReference());
                }
                releasedCount++;
            }
        }

        if (releasedCount > 0) {
            log.info("Cleanup completed: {} expired holds released out of {} active holds checked",
                    releasedCount, activeHolds.size());
        } else {
            log.debug("Cleanup completed: no expired holds found among {} active holds", activeHolds.size());
        }
    }
}
