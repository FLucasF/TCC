package com.loja.checkout.domain.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal LIMIAR_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_CREDITO).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMIAR_BRINDE) > 0;
    }
}
