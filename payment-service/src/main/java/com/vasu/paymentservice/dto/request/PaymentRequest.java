package com.vasu.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "Booking reference is required")
    private String bookingReference;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Currency is required")
    private String currency;
}
