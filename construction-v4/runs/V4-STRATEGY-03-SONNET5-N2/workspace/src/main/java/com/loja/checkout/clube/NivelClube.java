package com.loja.checkout.clube;

import java.math.BigDecimal;

import static java.math.RoundingMode.HALF_EVEN;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return BigDecimal.ZERO.setScale(2, HALF_EVEN);
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
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos.multiply(new BigDecimal("0.02")).setScale(2, HALF_EVEN);
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
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos.multiply(new BigDecimal("0.05")).setScale(2, HALF_EVEN);
        }

        @Override
        public boolean freteGratis() {
            return true;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    public abstract boolean freteGratis();

    public abstract boolean temBrinde(BigDecimal subtotalProdutos);
}
