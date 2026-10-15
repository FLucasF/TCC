package com.loja.checkout.regiao;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum SeguroRegiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal taxa;

    SeguroRegiao(BigDecimal taxa) {
        this.taxa = taxa;
    }

    public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(taxa).setScale(2, RoundingMode.HALF_EVEN);
    }
}
