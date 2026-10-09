package com.vasu.movie_service.controller;

import com.vasu.movie_service.dto.common.PageResponse;
import com.vasu.movie_service.dto.request.MovieCreateRequest;
import com.vasu.movie_service.dto.request.MovieUpdateRequest;
import com.vasu.movie_service.dto.response.MovieResponse;
import com.vasu.movie_service.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody MovieCreateRequest request) {
        return movieService.createMovie(request);
    }

    @GetMapping("/{id}")
    public MovieResponse getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id);
    }

    @GetMapping
    public PageResponse<MovieResponse> getAllMovies(
            @PageableDefault(size = 20) Pageable pageable) {
        Slice<MovieResponse> slice = movieService.getAllMovies(pageable);
        return toPageResponse(slice);
    }

    @GetMapping("/search")
    public PageResponse<MovieResponse> searchMovies(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        Slice<MovieResponse> slice = movieService.searchMovies(keyword, pageable);
        return toPageResponse(slice);
    }

    @PutMapping("/{id}")
    public MovieResponse updateMovie(@PathVariable Long id, @Valid @RequestBody MovieUpdateRequest request) {
        return movieService.updateMovie(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
    }

    private PageResponse<MovieResponse> toPageResponse(Slice<MovieResponse> slice) {
        return PageResponse.<MovieResponse>builder()
                .content(slice.getContent())
                .page(slice.getNumber())
                .size(slice.getSize())
                .first(slice.isFirst())
                .last(slice.isLast())
                .build();
    }
}
