package com.vasu.movie_service.config;

import com.cloudinary.Cloudinary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class CloudinaryConfig {

    @Value("${cloudinary.cloud_name:zqs7nwss}")
    private String cloudName;

    @Value("${cloudinary.api_key:291929936858848}")
    private String apiKey;

    @Value("${cloudinary.api_secret:c7NHkAx7IhWgcRKack1l3n08UJo}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);

        Cloudinary cloudinary = new Cloudinary(config);
        log.info("Cloudinary configured with cloud_name: {}", cloudName);
        return cloudinary;
    }
}
