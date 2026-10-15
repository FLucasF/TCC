package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal percentualCredito() {
            return BigDecimal.ZERO;
        }
    },
    PRATA {
        @Override
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.02");
        }
    },
    OURO {
        @Override
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.05");
        }

        @Override
        public boolean freteGratis() {
            return true;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500")) > 0;
        }
    };

    public abstract BigDecimal percentualCredito();

    public boolean freteGratis() {
        return false;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
