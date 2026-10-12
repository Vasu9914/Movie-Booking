package com.vasu.paymentservice.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("razorpay")
public class RazorpayConfig {

    private boolean enabled;
    private String keyId;
    private String keySecret;
    private String webhookSecret;

    @Bean
    public RazorpayClient razorpayClient() {
        if (!enabled) {
            return null;
        }
        try {
            return new RazorpayClient(keyId, keySecret);
        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to initialize Razorpay client", e);
        }
    }
}
