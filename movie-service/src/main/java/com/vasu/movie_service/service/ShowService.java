package com.vasu.movie_service.service;

import com.vasu.movie_service.dto.request.ShowCreateRequest;
import com.vasu.movie_service.dto.request.ShowStatusChangeRequest;
import com.vasu.movie_service.dto.request.ShowUpdateRequest;
import com.vasu.movie_service.dto.response.ShowResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;

public interface ShowService {

    ShowResponse createShow(ShowCreateRequest request);

    ShowResponse getShowById(Long id);

    Slice<ShowResponse> getAllShows(Pageable pageable);

    Slice<ShowResponse> getShowsByMovieId(Long movieId, Pageable pageable);

    Slice<ShowResponse> getShowsByCity(String city, Pageable pageable);

    Slice<ShowResponse> getShowsByMovieIdAndDate(Long movieId, LocalDateTime dateFrom, Pageable pageable);

    ShowResponse updateShow(Long id, ShowUpdateRequest request);

    void updateShowStatus(Long id, ShowStatusChangeRequest request);

    void cancelShow(Long id);
}
