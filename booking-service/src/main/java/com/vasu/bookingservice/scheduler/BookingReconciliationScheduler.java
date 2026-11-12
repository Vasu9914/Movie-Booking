package com.vasu.bookingservice.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vasu.bookingservice.entity.Booking;
import com.vasu.bookingservice.enums.BookingStatus;
import com.vasu.bookingservice.repository.BookingRepository;
import com.vasu.bookingservice.repository.ShowSeatRepository;
import com.vasu.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class BookingReconciliationScheduler {

    private static final int PENDING_MINUTES = 15;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final ShowSeatRepository showSeatRepository;

    @Scheduled(fixedDelay = 60000)
    public void reconcilePendingBookings() {
        log.info("Starting reconciliation for pending bookings older than {} minutes", PENDING_MINUTES);
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PENDING_MINUTES);
        List<Booking> pendingBookings = bookingRepository.findByStatusAndCreatedAtBefore(
                BookingStatus.PENDING_PAYMENT, threshold);

        if (!pendingBookings.isEmpty()) {
            log.info("Found {} pending bookings to reconcile", pendingBookings.size());
            for (Booking booking : pendingBookings) {
                try {
                    reconcileBooking(booking);
                } catch (Exception e) {
                    log.error("Failed to reconcile booking {}: {}", booking.getBookingReference(), e.getMessage());
                }
            }
        } else {
            log.info("No pending bookings to reconcile");
        }

        // Reconcile confirmed bookings where payment was actually refunded (lost refund.success message)
        reconcileRefundedBookings();

        // Expire old seat holds — always runs, regardless of pending bookings
        int expired = showSeatRepository.expireHolds();
        if (expired > 0) {
            log.info("Expired {} seat holds", expired);
        }
    }

    /**
     * Finds CONFIRMED bookings where the Payment Service says the payment was REFUNDED.
     * This fixes the case where refund.success RabbitMQ message was lost, leaving the
     * booking stuck as CONFIRMED with seats still BOOKED, even though money was refunded.
     */
    private void reconcileRefundedBookings() {
        log.info("Starting reconciliation for confirmed bookings with refunded payments");
        List<Booking> confirmedBookings = bookingRepository.findByStatus(BookingStatus.CONFIRMED);

        if (confirmedBookings.isEmpty()) {
            log.info("No confirmed bookings to reconcile");
            return;
        }

        log.info("Found {} confirmed bookings to check for refund mismatches", confirmedBookings.size());
        for (Booking booking : confirmedBookings) {
            try {
                reconcileRefundedBooking(booking);
            } catch (Exception e) {
                log.error("Failed to reconcile confirmed booking {}: {}", booking.getBookingReference(), e.getMessage());
            }
        }
    }

    private void reconcileRefundedBooking(Booking booking) {
        String bookingReference = booking.getBookingReference();
        String status = getPaymentStatusByBookingReference(bookingReference);

        if ("REFUNDED".equals(status)) {
            log.warn("Detected mismatch: booking {} is CONFIRMED but payment is REFUNDED. " +
                    "This means the refund.success message was lost. Fixing now.", bookingReference);
            bookingService.handleRefund(bookingReference);
            log.info("Booking {} reconciled: status changed to REFUNDED, seats released", bookingReference);
        } else if ("REFUND_PENDING".equals(status)) {
            log.info("Booking {} payment is REFUND_PENDING, waiting for refund to complete", bookingReference);
        }
        // SUCCESS, FAILED, NOT_FOUND — no action needed for confirmed bookings in these states
    }

    private void reconcileBooking(Booking booking) {
        String bookingReference = booking.getBookingReference();
        String status = getPaymentStatusByBookingReference(bookingReference);

        switch (status) {
            case "SUCCESS" -> {
                log.info("Reconciling booking {} as payment SUCCESS", bookingReference);
                bookingService.confirmBooking(bookingReference);
            }
            case "FAILED", "EXPIRED", "NOT_FOUND" -> {
                log.info("Reconciling booking {} as payment {}, cancelling", bookingReference, status);
                bookingService.cancelBookingForConsumer(bookingReference, "reconciliation-job");
            }
            default -> {
                log.info("Booking {} payment status is {}, skipping", bookingReference, status);
            }
        }
    }

    private String getPaymentStatusByBookingReference(String bookingReference) {
        try {
            String url = System.getProperty("payment.service.url", "http://localhost:8084")
                    + "/api/payments/status/" + bookingReference;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode node = MAPPER.readTree(response.body());
                return node.path("status").asText("ERROR");
            }
            if (response.statusCode() == 404) {
                return "NOT_FOUND";
            }
            return "ERROR";
        } catch (Exception e) {
            log.error("Failed to get payment status for booking {}: {}", bookingReference, e.getMessage());
            return "ERROR";
        }
    }
}
