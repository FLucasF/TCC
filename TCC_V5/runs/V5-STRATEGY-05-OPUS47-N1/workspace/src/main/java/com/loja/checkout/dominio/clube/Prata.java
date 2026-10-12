package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public class Prata implements NivelClube {
    @Override
    public BigDecimal fretePago(BigDecimal freteBase) {
        return Dinheiro.arredonda(freteBase);
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredonda(subtotalProdutos.multiply(new BigDecimal("0.02")));
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
