package com.vasu.bookingservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_EVENT_EXCHANGE = "payment.events";
    public static final String BOOKING_EVENT_EXCHANGE = "booking.events";
    public static final String PAYMENT_SUCCESS_QUEUE = "booking.payment.success.queue";
    public static final String PAYMENT_FAILED_QUEUE = "booking.payment.failed.queue";
    public static final String REFUND_SUCCESS_QUEUE = "booking.refund.success.queue";
    public static final String BOOKING_CANCELLED_QUEUE = "booking.cancelled.queue";
    public static final String PAYMENT_SUCCESS_ROUTING_KEY = "payment.success";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String REFUND_SUCCESS_ROUTING_KEY = "refund.success";
    public static final String BOOKING_CANCELLED_ROUTING_KEY = "booking.cancelled";
    public static final String BOOKING_REFUNDED_ROUTING_KEY = "booking.refunded";

    public static final String PAYMENT_DLX = "payment.dlx";
    public static final String PAYMENT_SUCCESS_DLQ = "booking.payment.success.dlq";
    public static final String PAYMENT_FAILED_DLQ = "booking.payment.failed.dlq";
    public static final String REFUND_SUCCESS_DLQ = "booking.refund.success.dlq";
    public static final String PAYMENT_SUCCESS_DLQ_ROUTING_KEY = "payment.success.dlq";
    public static final String PAYMENT_FAILED_DLQ_ROUTING_KEY = "payment.failed.dlq";
    public static final String REFUND_SUCCESS_DLQ_ROUTING_KEY = "refund.success.dlq";

    @Bean
    public DirectExchange paymentEventExchange() {
        return new DirectExchange(PAYMENT_EVENT_EXCHANGE);
    }

    @Bean
    public DirectExchange bookingEventExchange() {
        return new DirectExchange(BOOKING_EVENT_EXCHANGE);
    }

    @Bean
    public DirectExchange paymentDlx() {
        return new DirectExchange(PAYMENT_DLX);
    }

    @Bean
    public Queue paymentSuccessDlq() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_DLQ).build();
    }

    @Bean
    public Queue paymentFailedDlq() {
        return QueueBuilder.durable(PAYMENT_FAILED_DLQ).build();
    }

    @Bean
    public Queue refundSuccessDlq() {
        return QueueBuilder.durable(REFUND_SUCCESS_DLQ).build();
    }

    @Bean
    public Binding bindingPaymentSuccessDlq() {
        return BindingBuilder.bind(paymentSuccessDlq())
                .to(paymentDlx())
                .with(PAYMENT_SUCCESS_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding bindingPaymentFailedDlq() {
        return BindingBuilder.bind(paymentFailedDlq())
                .to(paymentDlx())
                .with(PAYMENT_FAILED_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding bindingRefundSuccessDlq() {
        return BindingBuilder.bind(refundSuccessDlq())
                .to(paymentDlx())
                .with(REFUND_SUCCESS_DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", PAYMENT_DLX)
                .withArgument("x-dead-letter-routing-key", PAYMENT_SUCCESS_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", PAYMENT_DLX)
                .withArgument("x-dead-letter-routing-key", PAYMENT_FAILED_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue refundSuccessQueue() {
        return QueueBuilder.durable(REFUND_SUCCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", PAYMENT_DLX)
                .withArgument("x-dead-letter-routing-key", REFUND_SUCCESS_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding bindingPaymentSuccess() {
        return BindingBuilder.bind(paymentSuccessQueue())
                .to(paymentEventExchange())
                .with(PAYMENT_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Binding bindingPaymentFailed() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(paymentEventExchange())
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingRefundSuccess() {
        return BindingBuilder.bind(refundSuccessQueue())
                .to(paymentEventExchange())
                .with(REFUND_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }

}
