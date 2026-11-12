package com.vasu.notificationservice.event;

import com.vasu.notificationservice.config.RabbitMQConfig;
import com.vasu.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventProcessor {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(PaymentEvent paymentEvent) {
        log.info("Received payment success event for booking: {}", paymentEvent.getBookingReference());
        try {
            notificationService.createNotification(
                    paymentEvent.getUserId(),
                    paymentEvent.getBookingReference(),
                    "EMAIL",
                    "user-" + paymentEvent.getUserId() + "@example.com",
                    "payment-success-" + paymentEvent.getPaymentReference()
            );
            log.info("Notification created for payment success: {}", paymentEvent.getPaymentReference());
        } catch (Exception e) {
            log.error("Failed to create notification for payment success: {}", paymentEvent.getPaymentReference(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE)
    public void handlePaymentFailed(PaymentEvent paymentEvent) {
        log.info("Received payment failed event for booking: {}", paymentEvent.getBookingReference());
        try {
            notificationService.createNotification(
                    paymentEvent.getUserId(),
                    paymentEvent.getBookingReference(),
                    "EMAIL",
                    "user-" + paymentEvent.getUserId() + "@example.com",
                    "payment-failed-" + paymentEvent.getPaymentReference()
            );
            log.info("Notification created for payment failure: {}", paymentEvent.getPaymentReference());
        } catch (Exception e) {
            log.error("Failed to create notification for payment failure: {}", paymentEvent.getPaymentReference(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.BOOKING_CANCELLED_QUEUE)
    public void handleBookingCancelled(BookingCancelledEvent event) {
        log.info("Received booking cancelled event for booking: {}", event.getBookingReference());
        try {
            notificationService.createNotification(
                    event.getUserId(),
                    event.getBookingReference(),
                    "EMAIL",
                    "user-" + event.getUserId() + "@example.com",
                    "booking-cancelled-" + event.getBookingReference()
            );
            log.info("Notification created for booking cancellation: {}", event.getBookingReference());
        } catch (Exception e) {
            log.error("Failed to create notification for booking cancellation: {}", event.getBookingReference(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.REFUND_SUCCESS_QUEUE)
    public void handleRefundSuccess(RefundEvent refundEvent) {
        log.info("Received refund success event for booking: {}", refundEvent.getBookingReference());
        try {
            notificationService.createNotification(
                    refundEvent.getUserId(),
                    refundEvent.getBookingReference(),
                    "EMAIL",
                    "user-" + refundEvent.getUserId() + "@example.com",
                    "refund-success-" + refundEvent.getRefundReference()
            );
            log.info("Notification created for refund success: {}", refundEvent.getRefundReference());
        } catch (Exception e) {
            log.error("Failed to create notification for refund success: {}", refundEvent.getRefundReference(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.REFUND_FAILED_QUEUE)
    public void handleRefundFailed(RefundEvent refundEvent) {
        log.info("Received refund failed event for booking: {}", refundEvent.getBookingReference());
        try {
            notificationService.createNotification(
                    refundEvent.getUserId(),
                    refundEvent.getBookingReference(),
                    "EMAIL",
                    "user-" + refundEvent.getUserId() + "@example.com",
                    "refund-failed-" + refundEvent.getRefundReference()
            );
            log.info("Notification created for refund failure: {}", refundEvent.getRefundReference());
        } catch (Exception e) {
            log.error("Failed to create notification for refund failure: {}", refundEvent.getRefundReference(), e);
        }
    }
}
