package com.vasu.bookingservice.event;

import com.vasu.bookingservice.dto.response.BookingResponse;
import com.vasu.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventProcessorTest {

    @Mock
    private BookingService bookingService;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BookingEventProcessor processor;

    @Test
    void handlePaymentSuccess_duplicateEvent_doesNotThrow() {
        PaymentEvent event = PaymentEvent.builder()
                .bookingReference("ref-123")
                .paymentReference("pay-123")
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status("SUCCESS")
                .correlationId("corr-1")
                .build();

        when(bookingService.confirmBooking("ref-123"))
                .thenReturn(BookingResponse.builder()
                        .bookingReference("ref-123")
                        .status("CONFIRMED")
                        .build());

        // First call
        processor.handlePaymentSuccess(event);

        // Second call (duplicate event)
        processor.handlePaymentSuccess(event);

        verify(bookingService, times(2)).confirmBooking("ref-123");
    }

    @Test
    void handlePaymentFailed_duplicateEvent_doesNotThrow() {
        PaymentEvent event = PaymentEvent.builder()
                .bookingReference("ref-123")
                .paymentReference("pay-123")
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status("FAILED")
                .correlationId("corr-1")
                .build();

        // First call
        processor.handlePaymentFailed(event);

        // Second call (duplicate event) — should not throw
        processor.handlePaymentFailed(event);

        verify(bookingService, times(2)).cancelBookingForConsumer("ref-123", "corr-1");
    }

    @Test
    void handlePaymentSuccess_forCancelledBooking_callsService() {
        PaymentEvent event = PaymentEvent.builder()
                .bookingReference("ref-cancelled")
                .paymentReference("pay-456")
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status("SUCCESS")
                .correlationId("corr-2")
                .build();

        when(bookingService.confirmBooking("ref-cancelled"))
                .thenReturn(BookingResponse.builder()
                        .bookingReference("ref-cancelled")
                        .status("PAYMENT_FAILED")
                        .build());

        processor.handlePaymentSuccess(event);

        verify(bookingService).confirmBooking("ref-cancelled");
    }

    @Test
    void handleRefundSuccess_duplicateEvent_doesNotThrow() {
        RefundEvent event = RefundEvent.builder()
                .bookingReference("ref-123")
                .refundReference("ref-123")
                .paymentReference("pay-123")
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .status("SUCCESS")
                .correlationId("corr-1")
                .build();

        // First call
        processor.handleRefundSuccess(event);

        // Second call (duplicate event)
        processor.handleRefundSuccess(event);

        verify(bookingService, times(2)).handleRefund("ref-123");
    }
}
