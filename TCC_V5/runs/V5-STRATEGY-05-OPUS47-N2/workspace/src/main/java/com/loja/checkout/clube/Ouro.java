package com.loja.checkout.clube;

import java.math.BigDecimal;

public final class Ouro implements NivelClube {
    private static final BigDecimal TAXA = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override public BigDecimal credito(BigDecimal subtotalProdutos) { return subtotalProdutos.multiply(TAXA); }
    @Override public boolean pagaFrete() { return false; }
    @Override public boolean temBrinde(BigDecimal subtotalProdutos) { return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0; }
}
