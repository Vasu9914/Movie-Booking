package com.vasu.movie_service.dto.request;

import com.vasu.movie_service.enums.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenSeatCreateRequest {

    @NotBlank(message = "Seat number is required")
    @Size(max = 10, message = "Seat number must not exceed 10 characters")
    private String seatNumber;

    @NotBlank(message = "Row label is required")
    @Size(max = 5, message = "Row label must not exceed 5 characters")
    private String rowLabel;

    @NotNull(message = "Column number is required")
    private Integer columnNumber;

    @NotNull(message = "Seat type is required")
    private SeatType seatType;
}
