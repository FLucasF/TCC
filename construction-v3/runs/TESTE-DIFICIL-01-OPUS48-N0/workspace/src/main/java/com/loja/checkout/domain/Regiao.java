package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Regiao do cliente. Define a porcentagem que a seguradora cobra sobre o valor
 * dos produtos. Para incluir uma regiao nova, basta adicionar aqui com sua
 * porcentagem.
 */
public enum Regiao {

    SUDESTE(new BigDecimal("0.010")),
    SUL(new BigDecimal("0.010")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.020"));

    private final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }
}
