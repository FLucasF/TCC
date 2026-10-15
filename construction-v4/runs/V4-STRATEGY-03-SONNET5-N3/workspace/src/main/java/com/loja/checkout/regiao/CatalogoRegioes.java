package com.loja.checkout.regiao;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CatalogoRegioes {

    private final Map<String, BigDecimal> percentualSeguro = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02")
    );

    public BigDecimal percentualSeguro(String regiao) {
        return percentualSeguro.get(regiao);
    }
}
