package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NivelPrata implements NivelClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean isentaFrete() {
        return false;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
