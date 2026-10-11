package com.vasu.movie_service.controller;

import com.vasu.movie_service.dto.common.PageResponse;
import com.vasu.movie_service.dto.request.ScreenCreateRequest;
import com.vasu.movie_service.dto.request.SeatLayoutRequest;
import com.vasu.movie_service.dto.request.StatusChangeRequest;
import com.vasu.movie_service.dto.request.TheatreCreateRequest;
import com.vasu.movie_service.dto.request.TheatreUpdateRequest;
import com.vasu.movie_service.dto.response.ScreenResponse;
import com.vasu.movie_service.dto.response.ScreenSeatResponse;
import com.vasu.movie_service.dto.response.TheatreResponse;
import com.vasu.movie_service.service.ScreenService;
import com.vasu.movie_service.service.TheatreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
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

import java.util.List;

@RestController
@RequestMapping("/api/theatres")
@RequiredArgsConstructor
public class TheatreController {

    private final TheatreService theatreService;
    private final ScreenService screenService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TheatreResponse createTheatre(@Valid @RequestBody TheatreCreateRequest request) {
        return theatreService.createTheatre(request);
    }

    @GetMapping("/{id}")
    public TheatreResponse getTheatreById(@PathVariable Long id) {
        return theatreService.getTheatreById(id);
    }

    @GetMapping
    public PageResponse<TheatreResponse> getAllTheatres(
            @RequestParam(required = false) String city,
            @PageableDefault(size = 20) Pageable pageable) {
        Slice<TheatreResponse> slice;
        if (city != null && !city.isBlank()) {
            slice = theatreService.getTheatresByCity(city, pageable);
        } else {
            slice = theatreService.getAllTheatres(pageable);
        }
        return toPageResponse(slice);
    }

    @PutMapping("/{id}")
    public TheatreResponse updateTheatre(@PathVariable Long id, @Valid @RequestBody TheatreUpdateRequest request) {
        return theatreService.updateTheatre(id, request);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTheatreStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        theatreService.updateTheatreStatus(id, request);
    }

    // --- Screens under this theatre ---

    @PostMapping("/{theatreId}/screens")
    @ResponseStatus(HttpStatus.CREATED)
    public ScreenResponse createScreen(@PathVariable Long theatreId,
                                        @Valid @RequestBody ScreenCreateRequest request) {
        return screenService.createScreen(theatreId, request);
    }

    @GetMapping("/{theatreId}/screens")
    public PageResponse<ScreenResponse> getScreensByTheatreId(
            @PathVariable Long theatreId,
            @PageableDefault(size = 20) Pageable pageable) {
        Slice<ScreenResponse> slice = screenService.getScreensByTheatreId(theatreId, pageable);
        return toScreenPageResponse(slice);
    }

    @PostMapping("/{theatreId}/screens/{screenId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ScreenSeatResponse> createSeats(@PathVariable Long screenId,
                                                 @Valid @RequestBody SeatLayoutRequest request) {
        return screenService.createSeats(screenId, request);
    }

    private PageResponse<ScreenResponse> toScreenPageResponse(Slice<ScreenResponse> slice) {
        return PageResponse.<ScreenResponse>builder()
                .content(slice.getContent())
                .page(slice.getNumber())
                .size(slice.getSize())
                .first(slice.isFirst())
                .last(slice.isLast())
                .build();
    }

    private PageResponse<TheatreResponse> toPageResponse(Slice<TheatreResponse> slice) {
        return PageResponse.<TheatreResponse>builder()
                .content(slice.getContent())
                .page(slice.getNumber())
                .size(slice.getSize())
                .first(slice.isFirst())
                .last(slice.isLast())
                .build();
    }
}
