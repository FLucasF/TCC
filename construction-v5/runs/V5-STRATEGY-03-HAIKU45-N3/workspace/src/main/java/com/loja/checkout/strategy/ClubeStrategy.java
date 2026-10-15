package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface ClubeStrategy {
    BigDecimal calcularCredito(BigDecimal subtotal);
    boolean temFreteGratis();
    boolean temBrinde(BigDecimal subtotal);
}
