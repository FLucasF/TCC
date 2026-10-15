package com.loja.estrategia;

import java.math.BigDecimal;

public class CupomMenos50 implements EstrategiaCupom {
    @Override
    public BigDecimal aplicar(BigDecimal subtotal, BigDecimal frete) {
        return new BigDecimal("50.00");
    }

    @Override
    public boolean validar(BigDecimal subtotal) {
        return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
    }
}
