package com.loja.checkout.entrega;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ModalidadesConfig {

    @Bean
    public Map<String, ModalidadeEntrega> modalidadesEntrega() {
        return Map.of(
                "ECONOMICA", new EntregaEconomica(),
                "EXPRESSA", new EntregaExpressa(),
                "RETIRADA_LOJA", new EntregaRetiradaLoja(),
                "MOTOBOY", new EntregaMotoboy()
        );
    }
}
