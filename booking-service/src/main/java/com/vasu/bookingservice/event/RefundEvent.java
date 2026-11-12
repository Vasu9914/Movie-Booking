package com.vasu.bookingservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundEvent {

    private String refundReference;
    private String paymentReference;
    private String bookingReference;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String providerRefundId;
    private String correlationId;
}
