package com.vasu.paymentservice.service.impl;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.vasu.paymentservice.config.RazorpayConfig;
import com.vasu.paymentservice.dto.request.PaymentRequest;
import com.vasu.paymentservice.dto.request.RazorpayVerifyRequest;
import com.vasu.paymentservice.dto.request.RefundRequest;
import com.vasu.paymentservice.dto.request.VerifyPaymentRequest;
import com.vasu.paymentservice.dto.request.WebhookPayload;
import com.vasu.paymentservice.dto.response.PaymentResponse;
import com.vasu.paymentservice.dto.response.RefundResponse;
import com.vasu.paymentservice.entity.Payment;
import com.vasu.paymentservice.entity.Refund;
import com.vasu.paymentservice.enums.PaymentStatus;
import com.vasu.paymentservice.enums.RefundStatus;
import com.vasu.paymentservice.event.PaymentEvent;
import com.vasu.paymentservice.event.PaymentEventPublisher;
import com.vasu.paymentservice.exception.ResourceNotFoundException;
import com.vasu.paymentservice.mapper.PaymentMapper;
import com.vasu.paymentservice.repository.PaymentRepository;
import com.vasu.paymentservice.repository.RefundRepository;
import com.vasu.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String MOCK_ORDER_PREFIX = "order_mock_";

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentEventPublisher paymentEventPublisher;
    private final RazorpayConfig razorpayConfig;
    private RazorpayClient razorpayClient;

    @Autowired(required = false)
    public void setRazorpayClient(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    // ------------------------------------------------------------------
    // Payment lifecycle
    // ------------------------------------------------------------------

    @Override
    public PaymentResponse initiatePayment(PaymentRequest request) {
        Payment existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElse(null);
        if (existing != null) {
            return paymentMapper.toPaymentResponse(existing);
        }

        // TODO: don't trust request.getAmount(); fetch/validate it against the booking service
        Payment payment = Payment.builder()
                .paymentReference(UUID.randomUUID().toString())
                .bookingReference(request.getBookingReference())
                .userId(request.getUserId())
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .build();
        return paymentMapper.toPaymentResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String paymentReference) {
        return paymentMapper.toPaymentResponse(findPayment(paymentReference));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByBookingReference(String bookingReference) {
        return paymentMapper.toPaymentResponseList(paymentRepository.findByBookingReference(bookingReference));
    }

    @Override
    public PaymentResponse retryPayment(String paymentReference) {
        Payment payment = findPayment(paymentReference);
        // Without webhooks nothing marks a payment FAILED, so PENDING can be retried too
        if (payment.getStatus() != PaymentStatus.FAILED && payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending or failed payments can be retried");
        }
        payment.setStatus(PaymentStatus.PENDING);
        return paymentMapper.toPaymentResponse(paymentRepository.save(payment));
    }

    // ------------------------------------------------------------------
    // Refunds
    // ------------------------------------------------------------------

    @Override
    public RefundResponse requestRefund(String paymentReference, RefundRequest request) {
        Payment payment = findPayment(paymentReference);
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Can only refund successful payments");
        }

        Refund existing = refundRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElse(null);
        if (existing != null) {
            return paymentMapper.toRefundResponse(existing);
        }

        // NOTE: this only records the refund as PENDING; it does not call Razorpay yet
        Refund refund = Refund.builder()
                .paymentId(payment.getId())
                .refundReference(UUID.randomUUID().toString())
                .idempotencyKey(request.getIdempotencyKey())
                .amount(request.getAmount())
                .status(RefundStatus.PENDING)
                .build();
        return paymentMapper.toRefundResponse(refundRepository.save(refund));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByPaymentReference(String paymentReference) {
        Payment payment = findPayment(paymentReference);
        return paymentMapper.toRefundResponseList(refundRepository.findByPaymentId(payment.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public RefundResponse getRefundByReference(String refundReference) {
        Refund refund = refundRepository.findByRefundReference(refundReference)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + refundReference));
        return paymentMapper.toRefundResponse(refund);
    }

    // ------------------------------------------------------------------
    // Razorpay: create order
    // ------------------------------------------------------------------

    @Override
    public Map<String, Object> createOrder(String paymentReference) {
        Payment payment = findPayment(paymentReference);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can create an order");
        }
        if (payment.getAmount() == null) {
            throw new IllegalStateException("Payment amount is not set for payment: " + paymentReference);
        }
        if (payment.getCurrency() == null || payment.getCurrency().isBlank()) {
            throw new IllegalStateException("Payment currency is not set for payment: " + paymentReference);
        }

        long amountInPaise = payment.getAmount().movePointRight(2).longValueExact();
        String currency = payment.getCurrency().toUpperCase();
        boolean realMode = razorpayConfig.isEnabled() && razorpayClient != null;

        // Reuse the existing real order so a double click doesn't create a second one
        String existingOrderId = payment.getRazorpayOrderId();
        if (realMode && existingOrderId != null && !existingOrderId.startsWith(MOCK_ORDER_PREFIX)) {
            return orderResponse(existingOrderId, amountInPaise, currency, paymentReference);
        }

        String orderId;
        if (realMode) {
            try {
                org.json.JSONObject orderRequest = new org.json.JSONObject();
                orderRequest.put("amount", amountInPaise);
                orderRequest.put("currency", currency);
                orderRequest.put("receipt", paymentReference);

                Order order = razorpayClient.orders.create(orderRequest);
                orderId = order.get("id");
                log.info("Razorpay order created: {} for payment: {}", orderId, paymentReference);
            } catch (RazorpayException e) {
                log.error("Failed to create Razorpay order for payment: {}", paymentReference, e);
                throw new RuntimeException("Failed to create payment order: " + e.getMessage(), e);
            }
        } else {
            orderId = MOCK_ORDER_PREFIX + System.currentTimeMillis();
            log.info("Mock order created: {} for payment: {}", orderId, paymentReference);
        }

        payment.setRazorpayOrderId(orderId);
        paymentRepository.save(payment);

        return orderResponse(orderId, amountInPaise, currency, paymentReference);
    }

    private Map<String, Object> orderResponse(String orderId, long amountInPaise,
                                              String currency, String paymentReference) {
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("amount", amountInPaise);
        response.put("currency", currency);
        response.put("paymentReference", paymentReference);
        response.put("keyId", razorpayConfig.getKeyId());
        return response;
    }

    // ------------------------------------------------------------------
    // Verification
    // ------------------------------------------------------------------

    /**
     * Mock-only verification by payment reference. Disabled when real Razorpay is on.
     */
    @Override
    public Map<String, Object> verifyPayment(VerifyPaymentRequest request) {
        if (razorpayConfig.isEnabled()) {
            return buildFailureResponse("Use the Razorpay verify endpoint");
        }

        String paymentReference = request.getPaymentReference();
        log.info("Mock payment verification for payment: {}", paymentReference);

        Payment payment = paymentRepository.findByPaymentReference(paymentReference).orElse(null);
        if (payment == null) {
            return buildFailureResponse("Payment record not found. Please initiate payment first.");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return buildSuccessResponse(payment);
        }

        String transactionId = "mock_payment_" + System.currentTimeMillis();
        markSuccess(payment, transactionId, null, null);
        return buildSuccessResponse(payment);
    }

    @Override
    public Map<String, Object> verifyRazorpayPayment(RazorpayVerifyRequest request) {
        String razorpayPaymentId = request.getRazorpayPaymentId();
        String razorpayOrderId = request.getRazorpayOrderId();
        String razorpaySignature = request.getRazorpaySignature();

        log.info("Verifying Razorpay payment: paymentId={}, orderId={}", razorpayPaymentId, razorpayOrderId);

        if (!razorpayConfig.isEnabled()) {
            log.info("Mock mode: skipping signature verification");
        } else {
            // HMAC-SHA256(order_id + "|" + payment_id, KEY_SECRET)
            String expectedSignature;
            try {
                expectedSignature = generateHmacSha256(razorpayOrderId + "|" + razorpayPaymentId,
                        razorpayConfig.getKeySecret());
            } catch (Exception e) {
                log.error("Error generating HMAC signature", e);
                return buildFailureResponse("Payment verification failed: signature generation error");
            }

            if (!constantTimeEquals(expectedSignature, razorpaySignature)) {
                log.warn("Invalid signature for paymentId={}, orderId={}", razorpayPaymentId, razorpayOrderId);
                return buildFailureResponse("Payment verification failed: invalid signature");
            }
            log.info("Razorpay signature verified for paymentId={}", razorpayPaymentId);
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null) {
            log.warn("No payment found for Razorpay order ID: {}", razorpayOrderId);
            return buildFailureResponse("Payment record not found for the given order ID");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already verified: {}", payment.getPaymentReference());
            return buildSuccessResponse(payment);
        }

        markSuccess(payment, razorpayPaymentId, razorpayPaymentId, razorpaySignature);
        return buildSuccessResponse(payment);
    }

    // ------------------------------------------------------------------
    // Webhook handling
    // ------------------------------------------------------------------

    @Override
    public void handleRazorpayWebhook(WebhookPayload payload, String signature) {
        if (payload == null || payload.getPayload() == null || payload.getPayload().getPayment() == null) {
            log.warn("Received webhook with missing payment data");
            return;
        }

        WebhookPayload.PaymentData paymentData = payload.getPayload().getPayment();
        String razorpayPaymentId = paymentData.getId();
        String razorpayOrderId = paymentData.getOrder_id();
        String status = paymentData.getStatus();

        log.info("Processing Razorpay webhook: paymentId={}, orderId={}, status={}",
                razorpayPaymentId, razorpayOrderId, status);

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null) {
            log.warn("No payment found for Razorpay order ID: {}", razorpayOrderId);
            return;
        }

        if ("captured".equals(status) || "paid".equals(status)) {
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                log.info("Payment already confirmed via webhook: {}", payment.getPaymentReference());
                return;
            }
            markSuccess(payment, razorpayPaymentId, razorpayPaymentId, signature);
            log.info("Payment confirmed via webhook: {} for booking: {}",
                    razorpayPaymentId, payment.getBookingReference());
        } else if ("failed".equals(status) || "refunded".equals(status)) {
            log.info("Payment {} with status {} for booking: {}",
                    razorpayPaymentId, status, payment.getBookingReference());
        } else {
            log.info("Unhandled payment status {} from webhook for order: {}", status, razorpayOrderId);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Payment findPayment(String paymentReference) {
        return paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentReference));
    }

    private void markSuccess(Payment payment, String transactionId,
                             String razorpayPaymentId, String razorpaySignature) {
        payment.setProviderTransactionId(transactionId);
        if (razorpayPaymentId != null) payment.setRazorpayPaymentId(razorpayPaymentId);
        if (razorpaySignature != null) payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        PaymentEvent event = PaymentEvent.builder()
                .paymentReference(payment.getPaymentReference())
                .bookingReference(payment.getBookingReference())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status("SUCCESS")
                .transactionId(transactionId)
                .build();
        paymentEventPublisher.publishPaymentSuccess(event);

        log.info("Payment confirmed: {} for booking: {}", transactionId, payment.getBookingReference());
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

    private String generateHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}