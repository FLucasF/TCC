package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Regiões do cliente. O seguro é sempre a mesma conta (percentual sobre o valor
 * dos produtos); só o percentual muda de região para região.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }
}
