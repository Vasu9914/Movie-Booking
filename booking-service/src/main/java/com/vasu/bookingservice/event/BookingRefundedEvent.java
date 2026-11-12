package com.vasu.bookingservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRefundedEvent {

    private String bookingReference;
    private Long userId;
    private String correlationId;
}
