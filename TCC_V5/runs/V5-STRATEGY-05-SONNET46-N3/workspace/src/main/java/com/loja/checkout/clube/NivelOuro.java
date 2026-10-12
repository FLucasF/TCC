package com.loja.checkout.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NivelOuro implements NivelClube {

    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public boolean isFreteGratis() {
        return true;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean ganharBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
