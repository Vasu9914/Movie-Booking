package com.vasu.bookingservice.event;

import com.vasu.bookingservice.config.RabbitMQConfig;
import com.vasu.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventProcessor {

    private final BookingService bookingService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(PaymentEvent paymentEvent) {
        log.info("Received payment success event for booking: {}", paymentEvent.getBookingReference());
        try {
            bookingService.confirmBooking(paymentEvent.getBookingReference());
            log.info("Booking confirmed: {}", paymentEvent.getBookingReference());
        } catch (Exception e) {
            log.error("Failed to confirm booking: {}", paymentEvent.getBookingReference(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(PaymentEvent paymentEvent) {
        log.info("Received payment failed event for booking: {}", paymentEvent.getBookingReference());
        try {
            bookingService.cancelBooking(paymentEvent.getBookingReference());
            log.info("Booking cancelled due to payment failure: {}", paymentEvent.getBookingReference());
        } catch (Exception e) {
            log.error("Failed to cancel booking after payment failure: {}", paymentEvent.getBookingReference(), e);
        }
    }

    public void publishBookingCancelled(BookingCancelledEvent event) {
        log.info("Publishing booking cancelled event for booking: {}", event.getBookingReference());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BOOKING_EVENT_EXCHANGE,
                RabbitMQConfig.BOOKING_CANCELLED_ROUTING_KEY,
                event
        );
    }
}
