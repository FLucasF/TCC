package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class NiveisClubeConfig {

    @Bean
    NivelClube bronze() {
        return new NivelClube("BRONZE", BigDecimal.ZERO, false, null);
    }

    @Bean
    NivelClube prata() {
        return new NivelClube("PRATA", new BigDecimal("2"), false, null);
    }

    @Bean
    NivelClube ouro() {
        return new NivelClube("OURO", new BigDecimal("5"), true, new BigDecimal("500.00"));
    }
}
