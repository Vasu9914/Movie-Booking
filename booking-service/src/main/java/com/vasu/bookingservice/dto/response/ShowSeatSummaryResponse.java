package com.vasu.bookingservice.dto.response;

import com.vasu.bookingservice.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeatSummaryResponse {

    private Long id;
    private Long showId;
    private Long screenSeatId;
    private BigDecimal price;
    private SeatStatus status;
    private int totalSeats;
    private int availableSeats;
    private int heldSeats;
    private int bookedSeats;
}
