package com.loja.checkout.clube;

import java.math.BigDecimal;

public class Bronze implements NivelClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return BigDecimal.ZERO.setScale(2);
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
