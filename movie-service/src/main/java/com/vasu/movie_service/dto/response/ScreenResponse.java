package com.vasu.movie_service.dto.response;

import com.vasu.movie_service.enums.EntityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenResponse {

    private Long id;
    private String name;
    private Integer totalSeats;
    private EntityStatus status;
    private TheatreResponse theatre;
}
