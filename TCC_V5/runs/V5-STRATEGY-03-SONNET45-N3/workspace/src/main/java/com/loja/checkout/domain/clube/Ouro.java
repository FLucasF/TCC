package com.loja.checkout.domain.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Ouro implements NivelClube {
    @Override
    public boolean zeraFrete() {
        return true;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.05"))
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }
}
