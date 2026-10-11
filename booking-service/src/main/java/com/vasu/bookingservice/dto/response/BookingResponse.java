package com.vasu.bookingservice.dto.response;

import com.vasu.bookingservice.enums.BookingStatus;
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
public class BookingResponse {

    private Long id;
    private String bookingReference;
    private Long userId;
    private Long showId;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
