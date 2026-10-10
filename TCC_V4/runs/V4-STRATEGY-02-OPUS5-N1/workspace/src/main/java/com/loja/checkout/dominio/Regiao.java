package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Onde o cliente mora. Muda so a porcentagem que a seguradora cobra; a conta do
 * seguro e a mesma em todas as regioes e fica em {@link Seguro}.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentualDoSeguro;

    Regiao(String percentualDoSeguro) {
        this.percentualDoSeguro = new BigDecimal(percentualDoSeguro);
    }

    BigDecimal percentualDoSeguro() {
        return percentualDoSeguro;
    }
}
