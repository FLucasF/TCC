package com.loja.resumo.calculo;

import java.math.BigDecimal;

import static com.loja.resumo.calculo.Dinheiro.arredonda;

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

    BigDecimal seguroSobre(BigDecimal subtotal) {
        return arredonda(subtotal.multiply(percentualSeguro));
    }
}
