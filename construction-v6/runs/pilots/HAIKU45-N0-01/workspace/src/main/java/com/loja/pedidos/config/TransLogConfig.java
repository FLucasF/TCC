package com.loja.pedidos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "translog")
@Data
public class TransLogConfig {
    private String url;
    private long timeoutMs;
}
