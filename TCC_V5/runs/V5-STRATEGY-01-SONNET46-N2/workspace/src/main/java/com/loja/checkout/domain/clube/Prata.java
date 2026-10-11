package com.loja.checkout.domain.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Prata implements NivelClube {

    private static final BigDecimal DOIS_PORCENTO = new BigDecimal("0.02");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return subtotal.multiply(DOIS_PORCENTO).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean brinde(BigDecimal subtotal) {
        return false;
    }
}
