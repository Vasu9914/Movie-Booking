package com.vasu.movie_service.dto.request;

import com.vasu.movie_service.enums.EntityStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieUpdateRequest {

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 50, message = "Language must not exceed 50 characters")
    private String language;

    @Min(value = 1, message = "Duration must be greater than 0")
    @Max(value = 300, message = "Duration must not exceed 300 minutes")
    private Integer durationMinutes;

    private LocalDate releaseDate;

    @Size(max = 1000, message = "Poster URL must not exceed 1000 characters")
    private String posterUrl;

    private EntityStatus status;
}
