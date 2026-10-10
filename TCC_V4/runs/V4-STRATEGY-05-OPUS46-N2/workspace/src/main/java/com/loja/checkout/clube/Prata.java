package com.loja.checkout.clube;

import java.math.BigDecimal;

public class Prata implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.02");

    @Override
    public BigDecimal creditoPercentual() {
        return CREDITO;
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
