package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }

        @Override
        public boolean freteGratis() {
            return false;
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
        }

        @Override
        public boolean freteGratis() {
            return false;
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO {
        private static final BigDecimal VALOR_MINIMO_BRINDE = new BigDecimal("500.00");

        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
        }

        @Override
        public boolean freteGratis() {
            return true;
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(VALOR_MINIMO_BRINDE) > 0;
        }
    };

    public abstract BigDecimal credito(BigDecimal subtotalProdutos);

    public abstract boolean freteGratis();

    public abstract boolean brinde(BigDecimal subtotalProdutos);
}
