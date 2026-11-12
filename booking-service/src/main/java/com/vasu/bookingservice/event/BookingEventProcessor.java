package com.vasu.bookingservice.event;

import com.vasu.bookingservice.config.RabbitMQConfig;
import com.vasu.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventProcessor {

    private final BookingService bookingService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(@Payload PaymentEvent paymentEvent) {
        bookingService.confirmBooking(paymentEvent.getBookingReference());
        log.info("Booking confirmed: {}", paymentEvent.getBookingReference());
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(@Payload PaymentEvent paymentEvent) {
        bookingService.cancelBookingForConsumer(paymentEvent.getBookingReference(), paymentEvent.getCorrelationId());
        log.info("Booking cancelled due to payment failure: {}", paymentEvent.getBookingReference());
    }

    @RabbitListener(queues = RabbitMQConfig.REFUND_SUCCESS_QUEUE)
    public void handleRefundSuccess(@Payload RefundEvent refundEvent) {
        bookingService.handleRefund(refundEvent.getBookingReference());
        log.info("Booking marked as refunded: {}", refundEvent.getBookingReference());
    }

    public void publishBookingCancelled(BookingCancelledEvent event) {
        log.info("Publishing booking cancelled event for booking: {}", event.getBookingReference());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BOOKING_EVENT_EXCHANGE,
                RabbitMQConfig.BOOKING_CANCELLED_ROUTING_KEY,
                event
        );
    }

    public void publishBookingRefunded(BookingRefundedEvent event) {
        log.info("Publishing booking refunded event for booking: {}", event.getBookingReference());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BOOKING_EVENT_EXCHANGE,
                RabbitMQConfig.BOOKING_REFUNDED_ROUTING_KEY,
                event
        );
    }
}
