package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false) {
        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },
    PRATA(new BigDecimal("0.02"), false) {
        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },
    OURO(new BigDecimal("0.05"), true) {
        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public abstract boolean temBrinde(BigDecimal subtotalProdutos);
}
