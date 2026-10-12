package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Regiao do cliente e o percentual de seguro cobrado pela seguradora.
 * A conta e a mesma em todas as regioes, so o percentual muda.
 */
public enum Regiao {

    SUDESTE("1.0"),
    SUL("1.0"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2.0");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }
}
