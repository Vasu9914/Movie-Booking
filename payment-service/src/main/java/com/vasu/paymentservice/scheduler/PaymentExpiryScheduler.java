package com.vasu.paymentservice.scheduler;

import com.vasu.paymentservice.entity.Payment;
import com.vasu.paymentservice.enums.PaymentStatus;
import com.vasu.paymentservice.repository.PaymentRepository;
import com.vasu.paymentservice.service.PaymentExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryScheduler {

    private static final int PAYMENT_TIMEOUT_MINUTES = 15;

    private final PaymentRepository paymentRepository;
    private final PaymentExpiryService expiryService;

    @Scheduled(fixedDelay = 60_000)
    public void expireStalePayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<Payment> pendingPayments = paymentRepository.findByStatusAndCreatedAtBefore(
                PaymentStatus.PENDING, threshold);

        if (pendingPayments.isEmpty()) {
            return;
        }

        log.info("Found {} pending payments older than {} minutes to expire",
                pendingPayments.size(), PAYMENT_TIMEOUT_MINUTES);

        for (Payment p : pendingPayments) {
            try {
                expiryService.expire(p);
            } catch (Exception e) {
                log.error("Failed to expire payment {}: {}", p.getPaymentReference(), e.getMessage());
            }
        }
    }
}
