package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class ClubeOuro implements ClubeStrategy {

    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.05"));
    }

    @Override
    public boolean temFreteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotal) {
        return subtotal.compareTo(LIMITE_BRINDE) > 0;
    }
}
