package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

public final class Bronze implements NivelClube {
    public BigDecimal credito(BigDecimal subtotalProdutos) { return Dinheiro.ZERO; }
    public boolean isentaFrete() { return false; }
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) { return false; }
}
