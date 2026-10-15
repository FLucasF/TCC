package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public interface NivelClube {

    BigDecimal calcularCredito(BigDecimal subtotal);

    boolean freteGratis();

    boolean brinde(BigDecimal subtotal);
}
