package com.loja.checkout.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NivelPrata implements NivelClube {

    @Override
    public boolean isFreteGratis() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean ganharBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
