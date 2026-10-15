package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;

import java.math.BigDecimal;

public class Ouro implements NivelClube {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal ajustaFrete(BigDecimal freteBase) { return Dinheiro.ZERO; }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredonda(subtotalProdutos.multiply(PERCENTUAL));
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
