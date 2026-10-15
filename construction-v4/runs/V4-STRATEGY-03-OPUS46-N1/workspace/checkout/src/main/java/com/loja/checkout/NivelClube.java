package com.loja.checkout;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false),

    PRATA(new BigDecimal("0.02"), false),

    OURO(new BigDecimal("0.05"), true) {
        @Override
        public boolean hasBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500")) > 0;
        }
    };

    private final BigDecimal taxaCashback;
    private final boolean freteGratis;

    NivelClube(BigDecimal taxaCashback, boolean freteGratis) {
        this.taxaCashback = taxaCashback;
        this.freteGratis = freteGratis;
    }

    public BigDecimal getTaxaCashback() {
        return taxaCashback;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean hasBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
