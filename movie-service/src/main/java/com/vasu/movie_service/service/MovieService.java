package com.vasu.movie_service.service;

import com.vasu.movie_service.dto.request.MovieCreateRequest;
import com.vasu.movie_service.dto.request.MovieUpdateRequest;
import com.vasu.movie_service.dto.response.MovieResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface MovieService {

    MovieResponse createMovie(MovieCreateRequest request);

    MovieResponse getMovieById(Long id);

    Slice<MovieResponse> getAllMovies(Pageable pageable);

    Slice<MovieResponse> searchMovies(String keyword, Pageable pageable);

    MovieResponse updateMovie(Long id, MovieUpdateRequest request);

    void deleteMovie(Long id);
}
