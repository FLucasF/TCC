package com.loja.resumo.domain;

import java.math.BigDecimal;

/**
 * O seguro cobra sempre a mesma conta (percentual sobre o valor dos produtos);
 * só o percentual muda de região para região, por isso é só um campo, não uma
 * estratégia por caso.
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

    public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(percentualSeguro));
    }
}
