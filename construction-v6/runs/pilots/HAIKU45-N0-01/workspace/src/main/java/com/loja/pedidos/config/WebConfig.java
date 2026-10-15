package com.loja.pedidos.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class WebConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, TransLogConfig config) {
        return builder
            .setConnectTimeout(Duration.ofMillis(config.getTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(config.getTimeoutMs()))
            .build();
    }
}
