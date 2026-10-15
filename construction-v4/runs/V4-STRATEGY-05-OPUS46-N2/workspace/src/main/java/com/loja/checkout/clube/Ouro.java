package com.loja.checkout.clube;

import java.math.BigDecimal;

public class Ouro implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal creditoPercentual() {
        return CREDITO;
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
