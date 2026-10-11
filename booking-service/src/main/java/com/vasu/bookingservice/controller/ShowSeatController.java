package com.vasu.bookingservice.controller;

import com.vasu.bookingservice.dto.request.ShowSeatCreateRequest;
import com.vasu.bookingservice.dto.response.SeatResponse;
import com.vasu.bookingservice.dto.response.ShowSeatSummaryResponse;
import com.vasu.bookingservice.service.ShowSeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/shows")
public class ShowSeatController {

    private final ShowSeatService showSeatService;

    @PostMapping("/{showId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public List<SeatResponse> createShowSeats(@PathVariable Long showId,
                                               @Valid @RequestBody ShowSeatCreateRequest request) {
        // Ensure the path variable matches the request body
        if (!showId.equals(request.getShowId())) {
            throw new IllegalArgumentException("Show ID in URL must match show ID in request body");
        }
        return showSeatService.createShowSeats(request);
    }

    @GetMapping("/{showId}/seats/summary")
    public ShowSeatSummaryResponse getShowSeatSummary(@PathVariable Long showId) {
        return showSeatService.getShowSeatSummary(showId);
    }

    @DeleteMapping("/{showId}/seats")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShowSeats(@PathVariable Long showId) {
        showSeatService.deleteShowSeatsByShowId(showId);
    }
}
