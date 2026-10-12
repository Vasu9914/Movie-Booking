package com.vasu.bookingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHoldInfo {

    private String holdReference;
    private Long showId;
    private List<Long> seatIds;
    private BigDecimal totalAmount;
    private LocalDateTime expiresAt;
    private String status;
    private Long ttlSeconds;
}
