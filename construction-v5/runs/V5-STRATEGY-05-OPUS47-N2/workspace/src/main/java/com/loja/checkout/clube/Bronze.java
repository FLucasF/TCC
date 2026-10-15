package com.loja.checkout.clube;

import java.math.BigDecimal;

public final class Bronze implements NivelClube {
    @Override public BigDecimal credito(BigDecimal subtotalProdutos) { return BigDecimal.ZERO; }
    @Override public boolean pagaFrete() { return true; }
    @Override public boolean temBrinde(BigDecimal subtotalProdutos) { return false; }
}
