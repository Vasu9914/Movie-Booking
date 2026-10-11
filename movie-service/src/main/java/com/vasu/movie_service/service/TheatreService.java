package com.vasu.movie_service.service;

import com.vasu.movie_service.dto.request.StatusChangeRequest;
import com.vasu.movie_service.dto.request.TheatreCreateRequest;
import com.vasu.movie_service.dto.request.TheatreUpdateRequest;
import com.vasu.movie_service.dto.response.TheatreResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface TheatreService {

    TheatreResponse createTheatre(TheatreCreateRequest request);

    TheatreResponse getTheatreById(Long id);

    Slice<TheatreResponse> getAllTheatres(Pageable pageable);

    Slice<TheatreResponse> getTheatresByCity(String city, Pageable pageable);

    TheatreResponse updateTheatre(Long id, TheatreUpdateRequest request);

    void updateTheatreStatus(Long id, StatusChangeRequest request);
}
