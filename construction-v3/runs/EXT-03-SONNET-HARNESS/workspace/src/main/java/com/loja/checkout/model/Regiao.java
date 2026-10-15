package com.loja.checkout.model;

import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

/** Regiões de imposto: a conta é sempre a mesma, só a porcentagem muda. */
public enum Regiao {

    SUDESTE(new BigDecimal("0.12")),
    SUL(new BigDecimal("0.11")),
    CENTRO_OESTE(new BigDecimal("0.09")),
    NORTE(new BigDecimal("0.07")),
    NORDESTE(new BigDecimal("0.07"));

    private final BigDecimal percentualImposto;

    Regiao(BigDecimal percentualImposto) {
        this.percentualImposto = percentualImposto;
    }

    public BigDecimal calcularImposto(BigDecimal baseComDesconto) {
        return Dinheiro.arredondar(baseComDesconto.multiply(percentualImposto));
    }
}
