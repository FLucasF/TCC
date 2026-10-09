package com.loja.checkout.calculo.cupom;

import java.math.BigDecimal;

public class FreteGratis implements CupomCalculador {
    @Override
    public boolean ehAplicavel(BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calcular(BigDecimal subtotal, BigDecimal frete) {
        return frete;
    }
}
