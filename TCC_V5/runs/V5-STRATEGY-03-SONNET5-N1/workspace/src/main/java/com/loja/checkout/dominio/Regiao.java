package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * O seguro do envio cobra uma porcentagem sobre os produtos: a formula e
 * sempre a mesma, apenas a porcentagem muda por regiao. Por isso a regiao
 * carrega o percentual como dado, sem precisar de uma implementacao por caso.
 */
public enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }
}
