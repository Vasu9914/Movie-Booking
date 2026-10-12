package com.vasu.bookingservice.controller;

import com.vasu.bookingservice.dto.common.PageResponse;
import com.vasu.bookingservice.dto.request.BookingCreateRequest;
import com.vasu.bookingservice.dto.request.BookingHoldRequest;
import com.vasu.bookingservice.dto.request.SeatHoldRequest;
import com.vasu.bookingservice.dto.response.BookingResponse;
import com.vasu.bookingservice.dto.response.BookingSeatResponse;
import com.vasu.bookingservice.dto.response.HoldResponse;
import com.vasu.bookingservice.dto.response.SeatResponse;
import com.vasu.bookingservice.dto.response.UserHoldInfo;
import com.vasu.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/api/shows/{showId}/seats")
    public List<SeatResponse> getSeatsByShowId(@PathVariable Long showId) {
        return bookingService.getSeatsByShowId(showId);
    }

    @PostMapping("/api/shows/{showId}/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse holdSeats(@PathVariable Long showId,
                                   @Valid @RequestBody SeatHoldRequest request) {
        return bookingService.holdSeats(showId, request);
    }

    @PostMapping("/api/bookings/holds")
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse holdSeatsByBooking(@Valid @RequestBody BookingHoldRequest request) {
        return bookingService.holdSeats(request);
    }

    @GetMapping("/api/bookings/holds/{holdReference}")
    public HoldResponse getHoldStatus(@PathVariable String holdReference) {
        return bookingService.getHoldStatus(holdReference);
    }

    @DeleteMapping("/api/bookings/holds/{holdReference}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseHold(@PathVariable String holdReference) {
        bookingService.releaseHold(holdReference);
    }

    @PostMapping("/api/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@RequestHeader("X-User-Id") Long userId,
                                          @Valid @RequestBody BookingCreateRequest request) {
        return bookingService.createBooking(userId, request);
    }

    @GetMapping("/api/bookings/{bookingReference}")
    public BookingResponse getBooking(@PathVariable String bookingReference) {
        return bookingService.getBookingByReference(bookingReference);
    }

    @GetMapping("/api/bookings/me")
    public PageResponse<BookingResponse> getUserBookings(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return bookingService.getUserBookings(userId, pageable);
    }

    @PostMapping("/api/bookings/{bookingReference}/confirm")
    public BookingResponse confirmBooking(@PathVariable String bookingReference) {
        return bookingService.confirmBooking(bookingReference);
    }

    @PostMapping("/api/bookings/{bookingReference}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBooking(@PathVariable String bookingReference) {
        bookingService.cancelBooking(bookingReference);
    }

    @GetMapping("/api/bookings/{bookingReference}/seats")
    public List<BookingSeatResponse> getBookingSeats(@PathVariable String bookingReference) {
        return bookingService.getBookingSeats(bookingReference);
    }

    @GetMapping("/api/bookings/holds/me")
    public List<UserHoldInfo> getUserActiveHolds(@RequestHeader("X-User-Id") Long userId) {
        return bookingService.getUserActiveHolds(userId);
    }

    @DeleteMapping("/api/bookings/holds/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void releaseAllUserHolds(@RequestHeader("X-User-Id") Long userId) {
        bookingService.releaseAllUserHolds(userId);
    }
}
