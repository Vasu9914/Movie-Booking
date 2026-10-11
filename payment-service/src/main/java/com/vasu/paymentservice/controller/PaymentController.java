package com.vasu.paymentservice.controller;

import com.vasu.paymentservice.dto.request.PaymentRequest;
import com.vasu.paymentservice.dto.request.RefundRequest;
import com.vasu.paymentservice.dto.request.VerifyPaymentRequest;
import com.vasu.paymentservice.dto.response.PaymentResponse;
import com.vasu.paymentservice.dto.response.RefundResponse;
import com.vasu.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

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
}
