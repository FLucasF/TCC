package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

class Bronze implements BeneficiosClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
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
