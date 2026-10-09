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
public class TheatreResponse {

    private Long id;
    private String name;
    private String city;
    private String address;
    private EntityStatus status;
}
