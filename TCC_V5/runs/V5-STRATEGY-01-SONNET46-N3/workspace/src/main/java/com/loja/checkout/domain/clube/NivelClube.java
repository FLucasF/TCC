package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public interface NivelClube {
    String codigo();
    boolean freteIsento();
    BigDecimal calcularCredito(BigDecimal subtotal);
    boolean brinde(BigDecimal subtotal);
}
