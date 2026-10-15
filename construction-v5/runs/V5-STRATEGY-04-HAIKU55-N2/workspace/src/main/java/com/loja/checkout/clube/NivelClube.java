package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BigDecimal percentualCredito();

    default BigDecimal aplicarFrete(BigDecimal frete) {
        return frete;
    }

    default boolean temBrinde(BigDecimal subtotal) {
        return false;
    }
}
