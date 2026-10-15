package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class Bronze implements Nivel {
    @Override public String codigo() { return "BRONZE"; }
    @Override public BigDecimal credito(BigDecimal subtotalProdutos) { return Dinheiro.ZERO; }
    @Override public BigDecimal ajustarFrete(BigDecimal fretePadrao) { return fretePadrao; }
    @Override public boolean brinde(BigDecimal subtotalProdutos) { return false; }
}
