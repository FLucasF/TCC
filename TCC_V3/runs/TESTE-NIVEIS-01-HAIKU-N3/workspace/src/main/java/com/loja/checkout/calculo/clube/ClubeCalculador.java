package com.loja.checkout.calculo.clube;

import java.math.BigDecimal;

public interface ClubeCalculador {
    BigDecimal calcularCredito(BigDecimal subtotal);
    boolean temFreteGratis();
    boolean temBrinde(BigDecimal subtotal);
}
