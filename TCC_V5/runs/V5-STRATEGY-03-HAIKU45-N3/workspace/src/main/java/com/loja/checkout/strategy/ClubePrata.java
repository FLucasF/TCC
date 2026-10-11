package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class ClubePrata implements ClubeStrategy {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.02"));
    }

    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotal) {
        return false;
    }
}
