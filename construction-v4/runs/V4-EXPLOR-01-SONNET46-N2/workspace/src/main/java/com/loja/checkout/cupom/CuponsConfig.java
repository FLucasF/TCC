package com.loja.checkout.cupom;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CuponsConfig {

    @Bean
    public Map<String, Cupom> cupons() {
        return Map.of(
                "BEMVINDO10", new CupomBemVindo10(),
                "MENOS50", new CupomMenos50(),
                "FRETEGRATIS", new CupomFreteGratis(),
                "LEVE3PAGUE2", new CupomLeve3Pague2()
        );
    }
}
