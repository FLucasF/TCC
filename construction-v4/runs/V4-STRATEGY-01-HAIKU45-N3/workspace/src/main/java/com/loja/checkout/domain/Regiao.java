package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum Regiao {
    SUDESTE(0.01),
    SUL(0.01),
    CENTRO_OESTE(0.015),
    NORTE(0.025),
    NORDESTE(0.02);

    private final BigDecimal taxaSeguro;

    Regiao(double taxaSeguro) {
        this.taxaSeguro = BigDecimal.valueOf(taxaSeguro);
    }

    public BigDecimal calcularSeguro(BigDecimal subtotal) {
        return subtotal.multiply(taxaSeguro);
    }
}
