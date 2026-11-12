package com.vasu.paymentservice.service;

import com.vasu.paymentservice.entity.Payment;
import com.vasu.paymentservice.enums.PaymentStatus;
import com.vasu.paymentservice.event.PaymentEvent;
import com.vasu.paymentservice.event.PaymentEventPublisher;
import com.vasu.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    public void expire(Payment p) {
        if (paymentRepository.expireIfPending(p.getId()) == 1) {
            log.info("Payment {} expired", p.getPaymentReference());
            PaymentEvent event = PaymentEvent.builder()
                    .paymentReference(p.getPaymentReference())
                    .bookingReference(p.getBookingReference())
                    .userId(p.getUserId())
                    .amount(p.getAmount())
                    .currency(p.getCurrency())
                    .status("EXPIRED")
                    .correlationId(UUID.randomUUID().toString())
                    .build();
            paymentEventPublisher.publishPaymentFailed(event);
        }
    }
}
