package com.loja.checkout.service.clube;

import java.math.BigDecimal;

public interface EstrategiaClube {
    String getCodigo();
    boolean freteGratis();
    BigDecimal calcularCredito(BigDecimal subtotal);
    boolean temBrinde(BigDecimal subtotal);
}
