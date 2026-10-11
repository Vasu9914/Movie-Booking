package com.vasu.movie_service.dto.response;

import com.vasu.movie_service.enums.SeatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenSeatResponse {

    private Long id;
    private String seatNumber;
    private String rowLabel;
    private Integer columnNumber;
    private SeatType seatType;
}
