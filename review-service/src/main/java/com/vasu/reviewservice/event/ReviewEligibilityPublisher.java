package com.vasu.reviewservice.event;

import com.vasu.reviewservice.config.RabbitMQConfig;
import com.vasu.reviewservice.exception.BookingVerificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEligibilityPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ConcurrentHashMap<String, CompletableFuture<ReviewEligibilityResponse>> pendingRequests = new ConcurrentHashMap<>();

    private static final int TIMEOUT_SECONDS = 10;

    /**
     * Sends an eligibility request to the Booking Service and waits for the response.
     */
    public boolean checkEligibility(Long userId, Long movieId) {
        String correlationId = UUID.randomUUID().toString();
        log.info("Sending review eligibility request for userId={}, movieId={}, correlationId={}", userId, movieId, correlationId);

        // Create a future to hold the response
        CompletableFuture<ReviewEligibilityResponse> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        // Send the request
        ReviewEligibilityRequest request = ReviewEligibilityRequest.builder()
                .correlationId(correlationId)
                .userId(userId)
                .movieId(movieId)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REVIEW_ELIGIBILITY_EXCHANGE,
                RabbitMQConfig.REVIEW_ELIGIBILITY_REQUEST_ROUTING_KEY,
                request
        );

        try {
            // Wait for the response
            ReviewEligibilityResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            pendingRequests.remove(correlationId);
            log.info("Received eligibility response for correlationId={}: eligible={}", correlationId, response.isEligible());
            return response.isEligible();
        } catch (TimeoutException e) {
            pendingRequests.remove(correlationId);
            log.error("Timeout waiting for eligibility response for correlationId={}", correlationId);
            throw new BookingVerificationException(
                    "Failed to verify booking eligibility: timeout waiting for Booking Service response"
            );
        } catch (InterruptedException e) {
            pendingRequests.remove(correlationId);
            Thread.currentThread().interrupt();
            log.error("Interrupted waiting for eligibility response for correlationId={}", correlationId);
            throw new BookingVerificationException(
                    "Failed to verify booking eligibility: request interrupted"
            );
        } catch (Exception e) {
            pendingRequests.remove(correlationId);
            log.error("Error waiting for eligibility response for correlationId={}", correlationId, e);
            throw new BookingVerificationException(
                    "Failed to verify booking eligibility: " + e.getMessage(), e
            );
        }
    }

    /**
     * Called by the listener when a response is received.
     */
    public void handleResponse(ReviewEligibilityResponse response) {
        String correlationId = response.getCorrelationId();
        CompletableFuture<ReviewEligibilityResponse> future = pendingRequests.get(correlationId);
        if (future != null) {
            future.complete(response);
        } else {
            log.warn("Received eligibility response for unknown correlationId={}", correlationId);
        }
    }
}
