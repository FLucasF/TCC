package com.loja.checkout.clube;

import java.math.BigDecimal;

public final class Prata implements NivelClube {
    private static final BigDecimal TAXA = new BigDecimal("0.02");

    @Override public BigDecimal credito(BigDecimal subtotalProdutos) { return subtotalProdutos.multiply(TAXA); }
    @Override public boolean pagaFrete() { return true; }
    @Override public boolean temBrinde(BigDecimal subtotalProdutos) { return false; }
}
