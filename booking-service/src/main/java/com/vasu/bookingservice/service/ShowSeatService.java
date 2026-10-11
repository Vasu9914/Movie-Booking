package com.vasu.bookingservice.service;

import com.vasu.bookingservice.dto.request.ShowSeatCreateRequest;
import com.vasu.bookingservice.dto.response.SeatResponse;
import com.vasu.bookingservice.dto.response.ShowSeatSummaryResponse;

import java.util.List;

public interface ShowSeatService {

    List<SeatResponse> createShowSeats(ShowSeatCreateRequest request);

    ShowSeatSummaryResponse getShowSeatSummary(Long showId);

    void deleteShowSeatsByShowId(Long showId);
}
