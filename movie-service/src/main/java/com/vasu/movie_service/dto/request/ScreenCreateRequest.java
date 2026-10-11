package com.vasu.movie_service.dto.request;

import com.vasu.movie_service.enums.EntityStatus;
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
public class ScreenCreateRequest {

    @NotBlank(message = "Screen name is required")
    @Size(max = 50, message = "Screen name must not exceed 50 characters")
    private String name;

    @NotNull(message = "Total seats is required")
    private Integer totalSeats;
}
