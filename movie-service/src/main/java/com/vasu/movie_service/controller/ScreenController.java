package com.vasu.movie_service.controller;

import com.vasu.movie_service.dto.common.PageResponse;
import com.vasu.movie_service.dto.request.ScreenCreateRequest;
import com.vasu.movie_service.dto.request.ScreenUpdateRequest;
import com.vasu.movie_service.dto.request.SeatLayoutRequest;
import com.vasu.movie_service.dto.request.StatusChangeRequest;
import com.vasu.movie_service.dto.response.ScreenResponse;
import com.vasu.movie_service.dto.response.ScreenSeatResponse;
import com.vasu.movie_service.service.ScreenService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/screens")
@RequiredArgsConstructor
public class ScreenController {

    private final ScreenService screenService;

    @GetMapping("/{id}")
    public ScreenResponse getScreenById(@PathVariable Long id) {
        return screenService.getScreenById(id);
    }

    @PutMapping("/{id}")
    public ScreenResponse updateScreen(@PathVariable Long id, @Valid @RequestBody ScreenUpdateRequest request) {
        return screenService.updateScreen(id, request);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateScreenStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        screenService.updateScreenStatus(id, request);
    }

    @PostMapping("/{screenId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ScreenSeatResponse> createSeats(@PathVariable Long screenId,
                                                 @Valid @RequestBody SeatLayoutRequest request) {
        return screenService.createSeats(screenId, request);
    }

    @GetMapping("/{screenId}/seats")
    public List<ScreenSeatResponse> getSeatsByScreenId(@PathVariable Long screenId) {
        return screenService.getSeatsByScreenId(screenId);
    }

    @GetMapping("/theatre/{theatreId}")
    @Deprecated
    public PageResponse<ScreenResponse> getScreensByTheatreId(
            @PathVariable Long theatreId,
            @PageableDefault(size = 20) Pageable pageable) {
        Slice<ScreenResponse> slice = screenService.getScreensByTheatreId(theatreId, pageable);
        return toPageResponse(slice);
    }

    @PostMapping("/theatre/{theatreId}")
    @Deprecated
    @ResponseStatus(HttpStatus.CREATED)
    public ScreenResponse createScreen(@PathVariable Long theatreId,
                                        @Valid @RequestBody ScreenCreateRequest request) {
        return screenService.createScreen(theatreId, request);
    }

    private PageResponse<ScreenResponse> toPageResponse(Slice<ScreenResponse> slice) {
        return PageResponse.<ScreenResponse>builder()
                .content(slice.getContent())
                .page(slice.getNumber())
                .size(slice.getSize())
                .first(slice.isFirst())
                .last(slice.isLast())
                .build();
    }
}
