package com.loja.estrategia;

import java.math.BigDecimal;

public interface EstrategiaCupom {
    BigDecimal aplicar(BigDecimal subtotal, BigDecimal frete);
    boolean validar(BigDecimal subtotal);
}
