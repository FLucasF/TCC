package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class Prata implements Nivel {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.02");

    @Override public String codigo() { return "PRATA"; }
    @Override public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(PERCENTUAL));
    }
    @Override public BigDecimal ajustarFrete(BigDecimal fretePadrao) { return fretePadrao; }
    @Override public boolean brinde(BigDecimal subtotalProdutos) { return false; }
}
