package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum Regiao {

    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal percentual;

    Regiao(BigDecimal percentual) {
        this.percentual = percentual;
    }

    public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(percentual).setScale(2, RoundingMode.HALF_EVEN);
    }
}
