package com.loja.checkout.regiao;

import java.math.BigDecimal;

/**
 * Regiões de entrega. A conta do seguro é igual em todas as regiões, só a
 * porcentagem muda — por isso é só uma tabela de valores, sem comportamento
 * próprio por região.
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
