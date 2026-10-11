package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

class Bronze implements NivelClube {

    @Override
    public String codigo() { return "BRONZE"; }

    @Override
    public boolean freteIsento() { return false; }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) { return new BigDecimal("0.00"); }

    @Override
    public boolean brinde(BigDecimal subtotal) { return false; }
}
