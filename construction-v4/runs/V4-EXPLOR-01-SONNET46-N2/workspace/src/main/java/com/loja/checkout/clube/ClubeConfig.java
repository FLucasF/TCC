package com.loja.checkout.clube;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;

@Configuration
public class ClubeConfig {

    @Bean
    public Map<String, BeneficiosClube> clube() {
        return Map.of(
                "BRONZE", new BeneficiosClube(BigDecimal.ZERO, false, null),
                "PRATA", new BeneficiosClube(new BigDecimal("0.02"), false, null),
                "OURO", new BeneficiosClube(new BigDecimal("0.05"), true, new BigDecimal("500.00"))
        );
    }
}
