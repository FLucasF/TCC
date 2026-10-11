package com.loja.checkout.domain.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Prata implements NivelClube {

    @Override
    public String codigo() { return "PRATA"; }

    @Override
    public boolean freteIsento() { return false; }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean brinde(BigDecimal subtotal) { return false; }
}
