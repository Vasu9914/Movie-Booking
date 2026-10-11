package com.vasu.movie_service.controller;

import com.vasu.movie_service.dto.common.PageResponse;
import com.vasu.movie_service.dto.request.ShowCreateRequest;
import com.vasu.movie_service.dto.request.ShowStatusChangeRequest;
import com.vasu.movie_service.dto.request.ShowUpdateRequest;
import com.vasu.movie_service.dto.response.ShowResponse;
import com.vasu.movie_service.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse createShow(@Valid @RequestBody ShowCreateRequest request) {
        return showService.createShow(request);
    }

    @GetMapping("/{id}")
    public ShowResponse getShowById(@PathVariable Long id) {
        return showService.getShowById(id);
    }

    @GetMapping
    public PageResponse<ShowResponse> getAllShows(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @PageableDefault(size = 20) Pageable pageable) {
        Slice<ShowResponse> slice;
        if (movieId != null && dateFrom != null) {
            slice = showService.getShowsByMovieIdAndDate(movieId, dateFrom, pageable);
        } else if (movieId != null) {
            slice = showService.getShowsByMovieId(movieId, pageable);
        } else if (city != null && !city.isBlank()) {
            slice = showService.getShowsByCity(city, pageable);
        } else {
            slice = showService.getAllShows(pageable);
        }
        return toPageResponse(slice);
    }

    @PutMapping("/{id}")
    public ShowResponse updateShow(@PathVariable Long id, @Valid @RequestBody ShowUpdateRequest request) {
        return showService.updateShow(id, request);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateShowStatus(@PathVariable Long id, @Valid @RequestBody ShowStatusChangeRequest request) {
        showService.updateShowStatus(id, request);
    }

    @PutMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelShow(@PathVariable Long id) {
        showService.cancelShow(id);
    }

    private PageResponse<ShowResponse> toPageResponse(Slice<ShowResponse> slice) {
        return PageResponse.<ShowResponse>builder()
                .content(slice.getContent())
                .page(slice.getNumber())
                .size(slice.getSize())
                .first(slice.isFirst())
                .last(slice.isLast())
                .build();
    }
}
