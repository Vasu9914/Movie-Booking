package com.vasu.bookingservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeatCreateRequest {

    @NotNull(message = "Show ID is required")
    private Long showId;

    @NotNull(message = "Seat list is required")
    @Size(min = 1, message = "At least one seat is required")
    private List<SeatItem> seats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatItem {
        @NotNull(message = "Screen seat ID is required")
        private Long screenSeatId;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        private BigDecimal price;
    }
}
