package com.loja.checkout.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.05");
    private static final BigDecimal LIMIAR_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return subtotal.multiply(PERCENTUAL).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean brinde(BigDecimal subtotal) {
        return subtotal.compareTo(LIMIAR_BRINDE) > 0;
    }
}
