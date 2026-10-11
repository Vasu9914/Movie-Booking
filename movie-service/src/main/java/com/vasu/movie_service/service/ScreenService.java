package com.vasu.movie_service.service;

import com.vasu.movie_service.dto.request.ScreenCreateRequest;
import com.vasu.movie_service.dto.request.ScreenUpdateRequest;
import com.vasu.movie_service.dto.request.SeatLayoutRequest;
import com.vasu.movie_service.dto.request.StatusChangeRequest;
import com.vasu.movie_service.dto.response.ScreenResponse;
import com.vasu.movie_service.dto.response.ScreenSeatResponse;
import com.vasu.movie_service.enums.EntityStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface ScreenService {

    ScreenResponse createScreen(Long theatreId, ScreenCreateRequest request);

    ScreenResponse getScreenById(Long id);

    Slice<ScreenResponse> getScreensByTheatreId(Long theatreId, Pageable pageable);

    ScreenResponse updateScreen(Long id, ScreenUpdateRequest request);

    void updateScreenStatus(Long id, StatusChangeRequest request);

    List<ScreenSeatResponse> createSeats(Long screenId, SeatLayoutRequest request);

    List<ScreenSeatResponse> getSeatsByScreenId(Long screenId);
}
