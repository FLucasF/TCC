package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

class Ouro implements BeneficiosClube {
    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(TAXA_CREDITO);
    }

    @Override
    public boolean isentoFrete() {
        return true;
    }

    @Override
    public boolean ganhabrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
