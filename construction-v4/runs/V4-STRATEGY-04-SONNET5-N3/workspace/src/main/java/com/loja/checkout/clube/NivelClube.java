package com.loja.checkout.clube;

import com.loja.checkout.service.Dinheiro;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.zero();
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
        }

        @Override
        public boolean isentaFrete() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO {
        @Override
        public BigDecimal credito(BigDecimal subtotalProdutos) {
            return Dinheiro.arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
        }

        @Override
        public boolean isentaFrete() {
            return true;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal credito(BigDecimal subtotalProdutos);

    public abstract boolean isentaFrete();

    public abstract boolean temBrinde(BigDecimal subtotalProdutos);
}
