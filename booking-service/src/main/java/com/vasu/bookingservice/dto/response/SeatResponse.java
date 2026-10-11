package com.vasu.bookingservice.dto.response;

import com.vasu.bookingservice.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {

    private Long id;
    private Long showId;
    private Long screenSeatId;
    private BigDecimal price;
    private SeatStatus status;
    private LocalDateTime reservedUntil;
}
