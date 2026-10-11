package com.loja.checkout.domain.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Ouro implements NivelClube {

    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() { return "OURO"; }

    @Override
    public boolean freteIsento() { return true; }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean brinde(BigDecimal subtotal) {
        return subtotal.compareTo(MINIMO_BRINDE) > 0;
    }
}
