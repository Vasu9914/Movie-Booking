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
public class ReviewEligibilityListener {

    private final BookingService bookingService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.REVIEW_ELIGIBILITY_REQUEST_QUEUE)
    public void handleEligibilityRequest(ReviewEligibilityRequest request) {
        log.info("Received review eligibility request for userId={}, movieId={}, correlationId={}",
                request.getUserId(), request.getMovieId(), request.getCorrelationId());

        try {
            boolean eligible = bookingService.hasValidBookingForMovie(
                    request.getUserId(), request.getMovieId()
            );

            ReviewEligibilityResponse response = ReviewEligibilityResponse.builder()
                    .correlationId(request.getCorrelationId())
                    .userId(request.getUserId())
                    .movieId(request.getMovieId())
                    .eligible(eligible)
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.REVIEW_ELIGIBILITY_EXCHANGE,
                    RabbitMQConfig.REVIEW_ELIGIBILITY_RESPONSE_ROUTING_KEY,
                    response
            );

            log.info("Sent eligibility response for correlationId={}: eligible={}",
                    request.getCorrelationId(), eligible);
        } catch (Exception e) {
            log.error("Error processing eligibility request for correlationId={}",
                    request.getCorrelationId(), e);

            // Send false response on error
            ReviewEligibilityResponse response = ReviewEligibilityResponse.builder()
                    .correlationId(request.getCorrelationId())
                    .userId(request.getUserId())
                    .movieId(request.getMovieId())
                    .eligible(false)
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.REVIEW_ELIGIBILITY_EXCHANGE,
                    RabbitMQConfig.REVIEW_ELIGIBILITY_RESPONSE_ROUTING_KEY,
                    response
            );
        }
    }
}
