package com.loja.checkout.resumo.clube;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/** Ganha 2% dos produtos de volta em crédito. */
public class Prata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(PERCENTUAL_CREDITO));
    }

    @Override
    public BigDecimal frete(BigDecimal freteCalculado) {
        return freteCalculado;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
