package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

public final class Prata implements NivelClube {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.02");

    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(PERCENTUAL));
    }

    public boolean isentaFrete() { return false; }

    public boolean ganhaBrinde(BigDecimal subtotalProdutos) { return false; }
}
