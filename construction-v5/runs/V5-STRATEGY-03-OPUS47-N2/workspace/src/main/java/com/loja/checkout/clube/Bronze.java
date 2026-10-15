package com.loja.checkout.clube;

import com.loja.checkout.Dinheiro;

import java.math.BigDecimal;

public class Bronze implements NivelClube {
    @Override
    public BigDecimal ajustaFrete(BigDecimal freteBase) { return freteBase; }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) { return Dinheiro.ZERO; }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) { return false; }
}
