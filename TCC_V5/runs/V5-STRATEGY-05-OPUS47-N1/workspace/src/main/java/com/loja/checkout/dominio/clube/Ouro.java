package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class Ouro implements NivelClube {
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500");

    @Override
    public BigDecimal fretePago(BigDecimal freteBase) {
        return Dinheiro.arredonda(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredonda(subtotalProdutos.multiply(new BigDecimal("0.05")));
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
