package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.ZERO;
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
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal credito(BigDecimal subtotalProdutos);

    public abstract boolean freteGratis();

    public abstract boolean brinde(BigDecimal subtotalProdutos);
}
