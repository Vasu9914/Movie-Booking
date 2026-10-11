package com.vasu.movie_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenUpdateRequest {

    @Size(max = 50, message = "Screen name must not exceed 50 characters")
    private String name;

    private Integer totalSeats;
}
