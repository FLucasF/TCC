package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos.multiply(new BigDecimal("0.02"));
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos.multiply(new BigDecimal("0.05"));
        }

        @Override
        public boolean isentaFrete() {
            return true;
        }

        @Override
        public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    public abstract boolean isentaFrete();
    public abstract boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
