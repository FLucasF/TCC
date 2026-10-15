package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NivelOuro implements NivelClube {

    private static final BigDecimal LIMIAR_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMIAR_BRINDE) > 0;
    }
}
