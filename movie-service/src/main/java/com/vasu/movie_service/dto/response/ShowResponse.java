package com.vasu.movie_service.dto.response;

import com.vasu.movie_service.enums.ShowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowResponse {

    private Long id;
    private MovieResponse movie;
    private ScreenResponse screen;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ShowStatus status;
    private LocalDateTime createdAt;
}
