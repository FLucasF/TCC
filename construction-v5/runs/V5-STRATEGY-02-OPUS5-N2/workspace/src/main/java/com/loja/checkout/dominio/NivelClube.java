package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cada nível do clube define o seu conjunto de vantagens.
 * Nível novo entra como uma constante nova.
 */
public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.ZERO;
        }
    },

    PRATA {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, Dinheiro.taxa("0.02"));
        }
    },

    OURO {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, Dinheiro.taxa("0.05"));
        }

        @Override
        public BigDecimal freteDevido(BigDecimal frete) {
            return Dinheiro.ZERO;
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    public BigDecimal freteDevido(BigDecimal frete) {
        return frete;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
