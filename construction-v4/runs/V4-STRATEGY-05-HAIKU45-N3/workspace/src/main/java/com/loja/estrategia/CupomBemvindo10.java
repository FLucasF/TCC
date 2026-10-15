package com.loja.estrategia;

import java.math.BigDecimal;

public class CupomBemvindo10 implements EstrategiaCupom {
    @Override
    public BigDecimal aplicar(BigDecimal subtotal, BigDecimal frete) {
        return subtotal.multiply(new BigDecimal("0.10"));
    }

    @Override
    public boolean validar(BigDecimal subtotal) {
        return true;
    }
}
