package com.vasu.paymentservice.service;

import com.vasu.paymentservice.dto.request.PaymentRequest;
import com.vasu.paymentservice.dto.request.RefundRequest;
import com.vasu.paymentservice.dto.request.VerifyPaymentRequest;
import com.vasu.paymentservice.dto.response.PaymentResponse;
import com.vasu.paymentservice.dto.response.RefundResponse;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    PaymentResponse initiatePayment(PaymentRequest request);

    Map<String, Object> createOrder(String paymentReference);

    PaymentResponse getPaymentByReference(String paymentReference);

    List<PaymentResponse> getPaymentsByBookingReference(String bookingReference);

    PaymentResponse retryPayment(String paymentReference);

    RefundResponse requestRefund(String paymentReference, RefundRequest request);

    List<RefundResponse> getRefundsByPaymentReference(String paymentReference);

    RefundResponse getRefundByReference(String refundReference);


    Map<String, Object> verifyPayment(VerifyPaymentRequest request);
}
