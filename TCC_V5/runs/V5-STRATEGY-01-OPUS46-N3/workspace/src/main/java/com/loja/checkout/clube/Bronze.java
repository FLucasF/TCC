package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

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
