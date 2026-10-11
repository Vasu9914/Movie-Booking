package com.vasu.paymentservice.service.impl;

import com.vasu.paymentservice.dto.request.PaymentRequest;
import com.vasu.paymentservice.dto.request.RefundRequest;
import com.vasu.paymentservice.dto.request.VerifyPaymentRequest;
import com.vasu.paymentservice.dto.response.PaymentResponse;
import com.vasu.paymentservice.dto.response.RefundResponse;
import com.vasu.paymentservice.entity.Payment;
import com.vasu.paymentservice.entity.Refund;
import com.vasu.paymentservice.enums.PaymentStatus;
import com.vasu.paymentservice.enums.RefundStatus;
import com.vasu.paymentservice.event.PaymentEvent;
import com.vasu.paymentservice.event.PaymentEventPublisher;
import com.vasu.paymentservice.exception.DuplicatePaymentException;
import com.vasu.paymentservice.exception.ResourceNotFoundException;
import com.vasu.paymentservice.mapper.PaymentMapper;
import com.vasu.paymentservice.repository.PaymentRepository;
import com.vasu.paymentservice.repository.RefundRepository;
import com.vasu.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentEventPublisher paymentEventPublisher;

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        Payment existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElse(null);
        if (existing != null) {
            return paymentMapper.toPaymentResponse(existing);
        }

        String paymentReference = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .paymentReference(paymentReference)
                .bookingReference(request.getBookingReference())
                .userId(request.getUserId())
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();
        Payment saved = paymentRepository.save(payment);
        return paymentMapper.toPaymentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentReference));
        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByBookingReference(String bookingReference) {
        List<Payment> payments = paymentRepository.findByBookingReference(bookingReference);
        return paymentMapper.toPaymentResponseList(payments);
    }

    @Override
    public PaymentResponse retryPayment(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentReference));
        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new IllegalStateException("Only failed payments can be retried");
        }
        payment.setStatus(PaymentStatus.PENDING);
        Payment updated = paymentRepository.save(payment);
        return paymentMapper.toPaymentResponse(updated);
    }

    @Override
    public RefundResponse requestRefund(String paymentReference, RefundRequest request) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentReference));
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Can only refund successful payments");
        }

        Refund existing = refundRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElse(null);
        if (existing != null) {
            return paymentMapper.toRefundResponse(existing);
        }

        String refundReference = UUID.randomUUID().toString();
        Refund refund = Refund.builder()
                .paymentId(payment.getId())
                .refundReference(refundReference)
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .status(RefundStatus.PENDING)
                .build();
        Refund saved = refundRepository.save(refund);
        return paymentMapper.toRefundResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByPaymentReference(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentReference));
        List<Refund> refunds = refundRepository.findByPaymentId(payment.getId());
        return paymentMapper.toRefundResponseList(refunds);
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefundByReference(String refundReference) {
        Refund refund = refundRepository.findByRefundReference(refundReference)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + refundReference));
        return paymentMapper.toRefundResponse(refund);
    }


    @Override
    public Map<String, Object> createOrder(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentReference));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can create an order");
        }

        if (payment.getAmount() == null) {
            throw new IllegalStateException("Payment amount is not set for payment: " + paymentReference);
        }

        if (payment.getCurrency() == null || payment.getCurrency().isBlank()) {
            throw new IllegalStateException("Payment currency is not set for payment: " + paymentReference);
        }

        // Mock mode: generate a fake order ID
        String mockOrderId = "order_mock_" + System.currentTimeMillis();

        log.info("Mock order created: {} for payment: {}", mockOrderId, paymentReference);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", mockOrderId);
        response.put("amount", payment.getAmount());
        response.put("currency", payment.getCurrency());
        response.put("paymentReference", paymentReference);
        return response;
    }


    @Override
    public Map<String, Object> verifyPayment(VerifyPaymentRequest request) {
        String paymentReference = request.getPaymentReference();

        // Mock mode: always return success
        log.info("Mock payment verification - automatically succeeding for payment: {}", paymentReference);

        // Find the payment record by payment reference
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElse(null);

        if (payment == null) {
            log.warn("No payment record found for reference: {}", paymentReference);
            return buildFailureResponse("Payment record not found. Please initiate payment first.");
        }

        // Check if payment is already successful (idempotency)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already verified for reference: {}", paymentReference);
            return buildSuccessResponse(payment);
        }

        // Update payment and mark as SUCCESS
        String transactionId = "mock_payment_" + System.currentTimeMillis();
        payment.setProviderTransactionId(transactionId);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // Publish payment success event via RabbitMQ
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .paymentReference(payment.getPaymentReference())
                .bookingReference(payment.getBookingReference())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status("SUCCESS")
                .transactionId(transactionId)
                .build();

        paymentEventPublisher.publishPaymentSuccess(paymentEvent);
        log.info("Mock payment verified and confirmed: {} for booking: {}",
                transactionId, payment.getBookingReference());

        return buildSuccessResponse(payment);
    }

    private Map<String, Object> buildFailureResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }

    private Map<String, Object> buildSuccessResponse(Payment payment) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Payment verified successfully");
        response.put("paymentReference", payment.getPaymentReference());
        response.put("bookingReference", payment.getBookingReference());
        return response;
    }

}
