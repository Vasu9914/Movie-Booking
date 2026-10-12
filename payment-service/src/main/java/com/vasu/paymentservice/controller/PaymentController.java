package com.vasu.paymentservice.controller;

import com.vasu.paymentservice.config.RazorpayConfig;
import com.vasu.paymentservice.dto.request.PaymentRequest;
import com.vasu.paymentservice.dto.request.RazorpayVerifyRequest;
import com.vasu.paymentservice.dto.request.RefundRequest;
import com.vasu.paymentservice.dto.request.VerifyPaymentRequest;
import com.vasu.paymentservice.dto.request.WebhookPayload;
import com.vasu.paymentservice.dto.response.PaymentResponse;
import com.vasu.paymentservice.dto.response.RefundResponse;
import com.vasu.paymentservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final RazorpayConfig razorpayConfig;

    @PostMapping("/api/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse initiatePayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.initiatePayment(request);
    }

    @PostMapping("/api/payments/{paymentReference}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createOrder(@PathVariable String paymentReference) {
        return paymentService.createOrder(paymentReference);
    }

    @GetMapping("/api/payments/{paymentReference}")
    public PaymentResponse getPayment(@PathVariable String paymentReference) {
        return paymentService.getPaymentByReference(paymentReference);
    }

    @GetMapping("/api/payments/by-booking/{bookingReference}")
    public List<PaymentResponse> getPaymentsByBooking(@PathVariable String bookingReference) {
        return paymentService.getPaymentsByBookingReference(bookingReference);
    }

    @PostMapping("/api/payments/{paymentReference}/retry")
    public PaymentResponse retryPayment(@PathVariable String paymentReference) {
        return paymentService.retryPayment(paymentReference);
    }

    @PostMapping("/api/payments/{paymentReference}/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    public RefundResponse requestRefund(@PathVariable String paymentReference,
                                         @Valid @RequestBody RefundRequest request) {
        return paymentService.requestRefund(paymentReference, request);
    }

    @GetMapping("/api/payments/{paymentReference}/refunds")
    public List<RefundResponse> getRefunds(@PathVariable String paymentReference) {
        return paymentService.getRefundsByPaymentReference(paymentReference);
    }

    @GetMapping("/api/refunds/{refundReference}")
    public RefundResponse getRefund(@PathVariable String refundReference) {
        return paymentService.getRefundByReference(refundReference);
    }

    /**
     * Verify payment (mock mode).
     * Request body: { paymentReference }
     * Response: { success: true/false, message, paymentReference, bookingReference }
     */
    @PostMapping("/api/payments/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        Map<String, Object> result = paymentService.verifyPayment(request);
        boolean success = (boolean) result.get("success");
        if (success) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Verify Razorpay payment signature.
     * Verifies HMAC-SHA256(order_id + "|" + payment_id, KEY_SECRET) against razorpay_signature.
     * Request body: { razorpayPaymentId, razorpayOrderId, razorpaySignature }
     * Response: { success: true/false, message, paymentReference, bookingReference }
     */
    @PostMapping("/api/payments/verify/razorpay")
    public ResponseEntity<Map<String, Object>> verifyRazorpayPayment(@Valid @RequestBody RazorpayVerifyRequest request) {
        Map<String, Object> result = paymentService.verifyRazorpayPayment(request);
        boolean success = (boolean) result.get("success");
        if (success) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Razorpay webhook endpoint to handle payment events.
     * Expects the X-Razorpay-Signature header for signature verification.
     */
    @PostMapping("/api/payments/webhooks/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            HttpServletRequest httpRequest) {

        log.info("Received Razorpay webhook");

        // Parse the raw body into WebhookPayload
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            WebhookPayload payload = objectMapper.readValue(rawBody, WebhookPayload.class);

            // Verify signature if provided
            if (signature != null && !signature.isEmpty()) {
                // For proper signature verification, we need the raw body
                // Generate expected signature from raw body
                String expectedSignature = generateHmacSha256(rawBody, getWebhookSecret());
                if (!constantTimeEquals(expectedSignature, signature)) {
                    log.warn("Invalid Razorpay webhook signature");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
                }
            }

            // Process the webhook
            paymentService.handleRazorpayWebhook(payload, signature);
            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    private String getWebhookSecret() {
        return razorpayConfig.getWebhookSecret();
    }

    private String generateHmacSha256(String data, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating HMAC signature", e);
            return "";
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
