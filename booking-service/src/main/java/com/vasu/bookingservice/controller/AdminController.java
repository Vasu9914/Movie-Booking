package com.vasu.bookingservice.controller;

import com.vasu.bookingservice.entity.SeatHold;
import com.vasu.bookingservice.enums.HoldStatus;
import com.vasu.bookingservice.repository.SeatHoldRepository;
import com.vasu.bookingservice.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final SeatHoldRepository seatHoldRepository;
    private final ShowSeatRepository showSeatRepository;

    /**
     * Release all expired holds and their seats back to AVAILABLE.
     * This is useful for testing when seats are stuck in HELD status.
     */
    @PostMapping("/expired-holds/release")
    @Transactional
    public ResponseEntity<Map<String, Object>> releaseExpiredHolds() {
        int holdsReleased = 0;
        int seatsReleased = 0;

        // Release holds that have expired by timestamp
        List<SeatHold> activeHolds = seatHoldRepository.findByStatus(HoldStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();

        for (SeatHold hold : activeHolds) {
            if (now.isAfter(hold.getExpiresAt())) {
                seatHoldRepository.updateStatusByHoldReference(hold.getHoldReference(), HoldStatus.EXPIRED);
                List<Long> seatIds = hold.getShowSeatIds();
                if (!seatIds.isEmpty()) {
                    seatsReleased += showSeatRepository.releaseSeats(seatIds);
                }
                holdsReleased++;
            }
        }

        // Also release any seats stuck in HELD status directly
        int directReleased = showSeatRepository.expireHolds();
        seatsReleased += directReleased;

        Map<String, Object> result = new HashMap<>();
        result.put("holdsReleased", holdsReleased);
        result.put("seatsReleased", seatsReleased);
        result.put("message", "Expired holds released successfully");

        log.info("Admin: Released {} expired holds and {} seats", holdsReleased, seatsReleased);
        return ResponseEntity.ok(result);
    }

    /**
     * Release ALL holds regardless of status — force reset for testing.
     */
    @PostMapping("/holds/release-all")
    @Transactional
    public ResponseEntity<Map<String, Object>> releaseAllHolds() {
        int holdsReleased = 0;
        int seatsReleased = 0;

        List<SeatHold> activeHolds = seatHoldRepository.findByStatus(HoldStatus.ACTIVE);

        for (SeatHold hold : activeHolds) {
            seatHoldRepository.updateStatusByHoldReference(hold.getHoldReference(), HoldStatus.RELEASED);
            List<Long> seatIds = hold.getShowSeatIds();
            if (!seatIds.isEmpty()) {
                seatsReleased += showSeatRepository.releaseSeats(seatIds);
            }
            holdsReleased++;
        }

        // Also release any seats stuck in HELD status directly
        int directReleased = showSeatRepository.expireHolds();
        seatsReleased += directReleased;

        Map<String, Object> result = new HashMap<>();
        result.put("holdsReleased", holdsReleased);
        result.put("seatsReleased", seatsReleased);
        result.put("message", "All holds released successfully");

        log.info("Admin: Force released {} holds and {} seats", holdsReleased, seatsReleased);
        return ResponseEntity.ok(result);
    }
}
