package com.loja.checkout.domain.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Prata implements NivelClube {
    @Override
    public boolean zeraFrete() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.02"))
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
