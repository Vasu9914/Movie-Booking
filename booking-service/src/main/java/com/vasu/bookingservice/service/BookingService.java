package com.vasu.bookingservice.service;

import com.vasu.bookingservice.dto.common.PageResponse;
import com.vasu.bookingservice.dto.request.BookingCreateRequest;
import com.vasu.bookingservice.dto.request.BookingHoldRequest;
import com.vasu.bookingservice.dto.request.SeatHoldRequest;
import com.vasu.bookingservice.dto.response.BookingResponse;
import com.vasu.bookingservice.dto.response.BookingSeatResponse;
import com.vasu.bookingservice.dto.response.HoldResponse;
import com.vasu.bookingservice.dto.response.SeatResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookingService {

    List<SeatResponse> getSeatsByShowId(Long showId);

    HoldResponse holdSeats(Long showId, SeatHoldRequest request);

    HoldResponse holdSeats(BookingHoldRequest request);

    HoldResponse getHoldStatus(String holdReference);

    BookingResponse createBooking(Long userId, BookingCreateRequest request);

    BookingResponse getBookingByReference(String reference);

    PageResponse<BookingResponse> getUserBookings(Long userId, Pageable pageable);

    BookingResponse confirmBooking(String bookingReference);

    void cancelBooking(String bookingReference);

    List<BookingSeatResponse> getBookingSeats(String bookingReference);

    void releaseHold(String holdReference);

    boolean hasValidBookingForMovie(Long userId, Long movieId);
}
