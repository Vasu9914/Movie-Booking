package com.vasu.paymentservice.dto.response;

import com.vasu.paymentservice.enums.RefundStatus;
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
public class RefundResponse {

    private Long id;
    private Long paymentId;
    private String refundReference;
    private BigDecimal amount;
    private RefundStatus status;
    private String providerRefundId;
    private LocalDateTime createdAt;
}
