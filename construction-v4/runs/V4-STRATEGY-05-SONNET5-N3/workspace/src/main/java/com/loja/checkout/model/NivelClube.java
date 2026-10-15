package com.loja.checkout.model;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal percentualCredito() {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean freteGratis() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.02");
        }

        @Override
        public boolean freteGratis() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
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
            return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
        }
    };

    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    public abstract BigDecimal percentualCredito();

    public abstract boolean freteGratis();

    public abstract boolean temBrinde(BigDecimal subtotalProdutos);

    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(percentualCredito().multiply(subtotalProdutos));
    }
}
