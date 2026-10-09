package com.vasu.movie_service.controller;

import com.vasu.movie_service.dto.common.PageResponse;
import com.vasu.movie_service.dto.request.ShowCreateRequest;
import com.vasu.movie_service.dto.response.ShowResponse;
import com.vasu.movie_service.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public PageResponse<ShowResponse> getAllShows(@PageableDefault(size = 20) Pageable pageable) {
        Slice<ShowResponse> slice = showService.getAllShows(pageable);
        return toPageResponse(slice);
    }

    @GetMapping("/movie/{movieId}")
    public PageResponse<ShowResponse> getShowsByMovieId(
            @PathVariable Long movieId,
            @PageableDefault(size = 20) Pageable pageable) {
        Slice<ShowResponse> slice = showService.getShowsByMovieId(movieId, pageable);
        return toPageResponse(slice);
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
