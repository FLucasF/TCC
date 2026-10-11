package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

public final class Ouro implements NivelClube {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(PERCENTUAL));
    }

    public boolean isentaFrete() { return true; }

    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
