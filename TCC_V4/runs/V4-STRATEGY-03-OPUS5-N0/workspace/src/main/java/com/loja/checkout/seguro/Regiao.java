package com.loja.checkout.seguro;

import java.math.BigDecimal;

/**
 * Regiao do cliente e o percentual que a seguradora cobra.
 * A conta do seguro e a mesma em todas: so o percentual muda.
 */
public enum Regiao {

    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }
}
