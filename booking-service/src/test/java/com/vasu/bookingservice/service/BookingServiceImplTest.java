package com.vasu.bookingservice.service;

import com.vasu.bookingservice.dto.response.BookingResponse;
import com.vasu.bookingservice.entity.Booking;
import com.vasu.bookingservice.enums.BookingStatus;
import com.vasu.bookingservice.event.BookingEventProcessor;
import com.vasu.bookingservice.mapper.BookingMapper;
import com.vasu.bookingservice.repository.BookingRepository;
import com.vasu.bookingservice.repository.BookingSeatRepository;
import com.vasu.bookingservice.repository.SeatHoldRepository;
import com.vasu.bookingservice.repository.ShowSeatRepository;
import com.vasu.bookingservice.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ShowSeatRepository showSeatRepository;
    @Mock
    private BookingSeatRepository bookingSeatRepository;
    @Mock
    private SeatHoldRepository seatHoldRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private BookingEventProcessor bookingEventProcessor;
    @Mock
    private RestTemplate restTemplate;

    private BookingServiceImpl bookingService;

    private Booking booking;
    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                showSeatRepository, bookingRepository, bookingSeatRepository,
                seatHoldRepository, bookingMapper, bookingEventProcessor, restTemplate);

        booking = Booking.builder()
                .id(1L)
                .bookingReference("ref-123")
                .userId(1L)
                .showId(1L)
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("100.00"))
                .currency("INR")
                .build();

        bookingResponse = BookingResponse.builder()
                .bookingReference("ref-123")
                .status("CONFIRMED")
                .build();
    }

    @Test
    void confirmBooking_idempotent_secondCallReturnsExisting() {
        when(bookingRepository.findByBookingReference("ref-123"))
                .thenReturn(Optional.of(booking));
        when(bookingMapper.toBookingResponse(any(Booking.class))).thenReturn(bookingResponse);

        // First call: PENDING_PAYMENT -> CONFIRMED
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        when(bookingRepository.updateStatusIfMatches(eq(1L), eq(BookingStatus.PENDING_PAYMENT), eq(BookingStatus.CONFIRMED)))
                .thenReturn(1);
        BookingResponse result1 = bookingService.confirmBooking("ref-123");
        assertNotNull(result1);

        // Second call: already CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findByBookingReference("ref-123"))
                .thenReturn(Optional.of(booking));
        BookingResponse result2 = bookingService.confirmBooking("ref-123");
        assertNotNull(result2);

        verify(bookingRepository, times(1)).updateStatusIfMatches(
                eq(1L), eq(BookingStatus.PENDING_PAYMENT), eq(BookingStatus.CONFIRMED));
    }

    @Test
    void cancelBookingForConsumer_idempotent_secondCallReturnsSilently() {
        when(bookingRepository.findByBookingReference("ref-123"))
                .thenReturn(Optional.of(booking));
        when(bookingSeatRepository.findByBookingId(1L)).thenReturn(Collections.emptyList());

        // First call: PENDING_PAYMENT -> CANCELLED
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        when(bookingRepository.updateStatusIfMatches(eq(1L), eq(BookingStatus.PENDING_PAYMENT), eq(BookingStatus.CANCELLED)))
                .thenReturn(1);
        bookingService.cancelBookingForConsumer("ref-123", "corr-1");

        // Second call: already CANCELLED — should return silently
        booking.setStatus(BookingStatus.CANCELLED);
        bookingService.cancelBookingForConsumer("ref-123", "corr-1");

        verify(bookingRepository, times(1)).updateStatusIfMatches(
                eq(1L), eq(BookingStatus.PENDING_PAYMENT), eq(BookingStatus.CANCELLED));
    }

    @Test
    void cancelBookingForConsumer_onConfirmedBooking_logsWarningAndReturns() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findByBookingReference("ref-123"))
                .thenReturn(Optional.of(booking));

        bookingService.cancelBookingForConsumer("ref-123", "corr-1");

        verify(bookingRepository, never()).updateStatusIfMatches(anyLong(), any(), any());
        verify(bookingEventProcessor, never()).publishBookingCancelled(any());
    }

    @Test
    void cancelBooking_restEndpoint_throwsOnAlreadyCancelled() {
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findByBookingReference("ref-123"))
                .thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () ->
                bookingService.cancelBooking("ref-123", "corr-1"));
    }
}
