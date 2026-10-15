package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public interface NivelClubeStrategy {
    String codigo();
    boolean isentoFrete();
    BigDecimal calcularCredito(BigDecimal subtotal);
    boolean contemBrinde(BigDecimal subtotal);
}
