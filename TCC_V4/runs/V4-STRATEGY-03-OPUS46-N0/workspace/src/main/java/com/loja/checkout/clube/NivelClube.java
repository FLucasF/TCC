package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BigDecimal calcularCredito(BigDecimal subtotal);

    default boolean freteGratis() {
        return false;
    }

    default boolean brinde(BigDecimal subtotal) {
        return false;
    }
}
