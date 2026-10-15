package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class EntregasConfig {

    @Bean
    Entrega economica() {
        return new EntregaPorPeso("ECONOMICA", new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null);
    }

    @Bean
    Entrega expressa() {
        return new EntregaPorPeso("EXPRESSA", new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null);
    }

    @Bean
    Entrega retiradaLoja() {
        return new EntregaPorPeso("RETIRADA_LOJA", BigDecimal.ZERO, BigDecimal.ZERO, 1, null);
    }

    @Bean
    Entrega motoboy() {
        return new EntregaPorPeso("MOTOBOY", new BigDecimal("18.00"), BigDecimal.ZERO, 0, new BigDecimal("5"));
    }
}
