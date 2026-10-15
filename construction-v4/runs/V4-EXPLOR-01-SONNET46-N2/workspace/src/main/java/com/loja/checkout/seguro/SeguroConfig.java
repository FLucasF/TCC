package com.loja.checkout.seguro;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;

@Configuration
public class SeguroConfig {

    @Bean
    public Map<String, BigDecimal> seguroPorRegiao() {
        return Map.of(
                "SUDESTE", new BigDecimal("0.01"),
                "SUL", new BigDecimal("0.01"),
                "CENTRO_OESTE", new BigDecimal("0.015"),
                "NORTE", new BigDecimal("0.025"),
                "NORDESTE", new BigDecimal("0.02")
        );
    }
}
