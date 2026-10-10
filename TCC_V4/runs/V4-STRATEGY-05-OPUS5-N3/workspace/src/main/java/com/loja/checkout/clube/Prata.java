package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** Ganha 2% dos produtos de volta em credito. */
public final class Prata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, PERCENTUAL_CREDITO);
    }
}
