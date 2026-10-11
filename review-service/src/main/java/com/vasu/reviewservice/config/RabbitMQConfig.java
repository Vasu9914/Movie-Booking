package com.vasu.reviewservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String REVIEW_ELIGIBILITY_EXCHANGE = "review.eligibility.exchange";
    public static final String REVIEW_ELIGIBILITY_REQUEST_QUEUE = "review.eligibility.request.queue";
    public static final String REVIEW_ELIGIBILITY_RESPONSE_QUEUE = "review.eligibility.response.queue";
    public static final String REVIEW_ELIGIBILITY_REQUEST_ROUTING_KEY = "review.eligibility.request";
    public static final String REVIEW_ELIGIBILITY_RESPONSE_ROUTING_KEY = "review.eligibility.response";

    @Bean
    public DirectExchange reviewEligibilityExchange() {
        return new DirectExchange(REVIEW_ELIGIBILITY_EXCHANGE);
    }

    @Bean
    public Queue reviewEligibilityRequestQueue() {
        return new Queue(REVIEW_ELIGIBILITY_REQUEST_QUEUE, true);
    }

    @Bean
    public Queue reviewEligibilityResponseQueue() {
        return new Queue(REVIEW_ELIGIBILITY_RESPONSE_QUEUE, true);
    }

    @Bean
    public Binding bindRequestToExchange() {
        return BindingBuilder.bind(reviewEligibilityRequestQueue())
                .to(reviewEligibilityExchange())
                .with(REVIEW_ELIGIBILITY_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding bindResponseToExchange() {
        return BindingBuilder.bind(reviewEligibilityResponseQueue())
                .to(reviewEligibilityExchange())
                .with(REVIEW_ELIGIBILITY_RESPONSE_ROUTING_KEY);
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

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
