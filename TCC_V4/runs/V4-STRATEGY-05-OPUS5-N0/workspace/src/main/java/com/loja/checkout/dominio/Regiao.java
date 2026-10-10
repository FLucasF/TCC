package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Regiao do cliente. O seguro de extravio e roubo e sempre a mesma conta
 * (percentual sobre o valor dos produtos); so o percentual muda por regiao.
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
