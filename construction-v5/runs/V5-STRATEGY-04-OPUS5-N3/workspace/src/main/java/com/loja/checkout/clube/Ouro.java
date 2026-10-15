package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Ouro: 5% em crédito, nunca paga frete e ganha brinde acima de R$ 500,00 em produtos. */
class Ouro implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, CREDITO);
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
