package com.vasu.movie_service.service;

import com.vasu.movie_service.dto.request.ShowCreateRequest;
import com.vasu.movie_service.dto.response.ShowResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ShowService {

    ShowResponse createShow(ShowCreateRequest request);

    ShowResponse getShowById(Long id);

    Slice<ShowResponse> getAllShows(Pageable pageable);

    Slice<ShowResponse> getShowsByMovieId(Long movieId, Pageable pageable);

    void cancelShow(Long id);
}
