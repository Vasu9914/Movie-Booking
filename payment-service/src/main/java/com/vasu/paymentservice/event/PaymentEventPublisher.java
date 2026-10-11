package com.vasu.paymentservice.event;

import com.vasu.paymentservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentSuccess(PaymentEvent paymentEvent) {
        log.info("Publishing payment success event for payment: {}", paymentEvent.getPaymentReference());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EVENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY,
                paymentEvent
        );
    }

    public void publishPaymentFailed(PaymentEvent paymentEvent) {
        log.info("Publishing payment failed event for payment: {}", paymentEvent.getPaymentReference());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PAYMENT_EVENT_EXCHANGE,
                RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY,
                paymentEvent
        );
    }
}
