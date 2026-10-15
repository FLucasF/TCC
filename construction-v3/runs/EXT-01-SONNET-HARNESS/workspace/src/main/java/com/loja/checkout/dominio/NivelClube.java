package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE {
        @Override
        public boolean freteGratis() {
            return false;
        }

        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public boolean freteGratis() {
            return false;
        }

        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos.multiply(new BigDecimal("0.02"));
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO {
        @Override
        public boolean freteGratis() {
            return true;
        }

        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos.multiply(new BigDecimal("0.05"));
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract boolean freteGratis();

    public abstract BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    public abstract boolean temBrinde(BigDecimal subtotalProdutos);
}
