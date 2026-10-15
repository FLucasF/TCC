package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class Ouro implements Nivel {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override public String codigo() { return "OURO"; }
    @Override public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(PERCENTUAL));
    }
    @Override public BigDecimal ajustarFrete(BigDecimal fretePadrao) { return Dinheiro.ZERO; }
    @Override public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
