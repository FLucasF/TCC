package com.loja.checkout.calculo.cupom;

import java.math.BigDecimal;

public interface CupomCalculador {
    boolean ehAplicavel(BigDecimal subtotal);
    BigDecimal calcular(BigDecimal subtotal, BigDecimal frete);
}
