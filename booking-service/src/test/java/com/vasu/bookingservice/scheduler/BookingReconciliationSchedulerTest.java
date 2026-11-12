package com.vasu.bookingservice.scheduler;

import com.vasu.bookingservice.entity.Booking;
import com.vasu.bookingservice.enums.BookingStatus;
import com.vasu.bookingservice.repository.BookingRepository;
import com.vasu.bookingservice.repository.ShowSeatRepository;
import com.vasu.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingReconciliationSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private ShowSeatRepository showSeatRepository;

    @InjectMocks
    private BookingReconciliationScheduler scheduler;

    @Test
    void reconcile_noPendingBookings_doesNothing() {
        when(bookingRepository.findByStatusAndCreatedAtBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        scheduler.reconcilePendingBookings();

        verify(bookingService, never()).confirmBooking(anyString());
        verify(bookingService, never()).cancelBookingForConsumer(anyString(), anyString());
    }

    @Test
    void reconcile_oneBookingFails_nextBookingStillProcessed() {
        Booking booking1 = Booking.builder()
                .id(1L)
                .bookingReference("ref-1")
                .status(BookingStatus.PENDING_PAYMENT)
                .createdAt(LocalDateTime.now().minusMinutes(15))
                .build();
        Booking booking2 = Booking.builder()
                .id(2L)
                .bookingReference("ref-2")
                .status(BookingStatus.PENDING_PAYMENT)
                .createdAt(LocalDateTime.now().minusMinutes(15))
                .build();

        when(bookingRepository.findByStatusAndCreatedAtBefore(any(), any()))
                .thenReturn(Arrays.asList(booking1, booking2));

        // First booking throws an exception, second booking should still be processed
        doThrow(new RuntimeException("Payment service down"))
                .doNothing()
                .when(bookingService).confirmBooking(anyString());

        scheduler.reconcilePendingBookings();

        // Both bookings should have been attempted
        verify(bookingService, times(2)).confirmBooking(anyString());
    }

    @Test
    void reconcile_expiredSeatHolds_areExpired() {
        when(bookingRepository.findByStatusAndCreatedAtBefore(any(), any()))
                .thenReturn(Collections.emptyList());
        when(showSeatRepository.expireHolds()).thenReturn(5);

        scheduler.reconcilePendingBookings();

        verify(showSeatRepository).expireHolds();
    }
}
