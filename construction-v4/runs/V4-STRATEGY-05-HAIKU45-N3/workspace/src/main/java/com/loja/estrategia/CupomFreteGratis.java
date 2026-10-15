package com.loja.estrategia;

import java.math.BigDecimal;

public class CupomFreteGratis implements EstrategiaCupom {
    @Override
    public BigDecimal aplicar(BigDecimal subtotal, BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean validar(BigDecimal subtotal) {
        return true;
    }
}
