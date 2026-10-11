package com.vasu.reviewservice.event;

import com.vasu.reviewservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEligibilityListener {

    private final ReviewEligibilityPublisher publisher;

    @RabbitListener(queues = RabbitMQConfig.REVIEW_ELIGIBILITY_RESPONSE_QUEUE)
    public void handleEligibilityResponse(ReviewEligibilityResponse response) {
        log.info("Received eligibility response for correlationId={}, eligible={}",
                response.getCorrelationId(), response.isEligible());
        publisher.handleResponse(response);
    }
}
