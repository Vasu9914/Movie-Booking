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
public class HoldResponse {

    private String holdReference;
    private List<SeatResponse> seats;
    private LocalDateTime expiresAt;
    private BigDecimal totalAmount;
}
