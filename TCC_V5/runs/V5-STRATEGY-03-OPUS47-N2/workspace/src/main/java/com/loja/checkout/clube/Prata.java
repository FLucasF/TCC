package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;

import java.math.BigDecimal;

public class Prata implements NivelClube {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.02");

    @Override
    public BigDecimal ajustaFrete(BigDecimal freteBase) { return freteBase; }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredonda(subtotalProdutos.multiply(PERCENTUAL));
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) { return false; }
}
