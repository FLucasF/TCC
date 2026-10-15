package com.loja.checkout.domain;

import java.math.BigDecimal;

import static com.loja.checkout.domain.Arredondamento.r;

public enum Regiao {

    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal taxa;

    Regiao(BigDecimal taxa) {
        this.taxa = taxa;
    }

    public BigDecimal calcularSeguro(BigDecimal subtotal) {
        return r(subtotal.multiply(taxa));
    }
}
