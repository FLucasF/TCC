package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

class Prata implements BeneficiosClube {
    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.02");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(TAXA_CREDITO);
    }

    @Override
    public boolean isentoFrete() {
        return false;
    }

    @Override
    public boolean ganhabrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
