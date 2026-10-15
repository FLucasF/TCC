package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Prata: 2% dos produtos em crédito para a próxima compra. */
class Prata implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, CREDITO);
    }
}
