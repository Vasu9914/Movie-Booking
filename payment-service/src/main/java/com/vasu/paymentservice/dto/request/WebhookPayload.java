package com.vasu.paymentservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {

    private String entity;
    private Long account_id;
    private Long event;
    private Long amount;
    private String currency;
    private Map<String, Object> metadata;
    private PayloadData payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayloadData {
        private PaymentData payment;
        private OrderData order;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentData {
        private String id;
        private String entity;
        private Long amount;
        private String currency;
        private String status;
        private String order_id;
        private String method;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderData {
        private String id;
        private String entity;
    }
}
