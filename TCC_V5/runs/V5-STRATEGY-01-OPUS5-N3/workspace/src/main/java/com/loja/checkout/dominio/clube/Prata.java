package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

public final class Prata implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, CREDITO);
    }

    @Override
    public BigDecimal freteCobrado(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
