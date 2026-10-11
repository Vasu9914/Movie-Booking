package com.vasu.movie_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayoutRequest {

    @NotEmpty(message = "Seat list must not be empty")
    @Valid
    private List<ScreenSeatCreateRequest> seats;
}
