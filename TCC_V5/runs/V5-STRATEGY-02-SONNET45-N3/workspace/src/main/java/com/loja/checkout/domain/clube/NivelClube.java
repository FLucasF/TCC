package com.loja.checkout.domain.clube;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public enum NivelClube {
    BRONZE {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public boolean isentarFrete() {
            return false;
        }

        @Override
        public boolean concederBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            BigDecimal credito = subtotalProdutos.multiply(new BigDecimal("0.02"));
            return Arredondamento.arredondar(credito);
        }

        @Override
        public boolean isentarFrete() {
            return false;
        }

        @Override
        public boolean concederBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            BigDecimal credito = subtotalProdutos.multiply(new BigDecimal("0.05"));
            return Arredondamento.arredondar(credito);
        }

        @Override
        public boolean isentarFrete() {
            return true;
        }

        @Override
        public boolean concederBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    public abstract boolean isentarFrete();
    public abstract boolean concederBrinde(BigDecimal subtotalProdutos);
}
