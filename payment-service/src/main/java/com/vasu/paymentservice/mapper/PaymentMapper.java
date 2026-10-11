package com.vasu.paymentservice.mapper;

import com.vasu.paymentservice.dto.response.PaymentResponse;
import com.vasu.paymentservice.dto.response.RefundResponse;
import com.vasu.paymentservice.entity.Payment;
import com.vasu.paymentservice.entity.Refund;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentResponse toPaymentResponse(Payment payment);
    List<PaymentResponse> toPaymentResponseList(List<Payment> payments);
    RefundResponse toRefundResponse(Refund refund);
    List<RefundResponse> toRefundResponseList(List<Refund> refunds);
}
