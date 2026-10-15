package com.loja.checkout.strategy;

import java.math.BigDecimal;

public class ClubeBronze implements ClubeStrategy {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return new BigDecimal("0.00");
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
