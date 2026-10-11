package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Niveis do clube. Cada nivel traz o proprio conjunto de vantagens; entra
 * nivel novo sem mexer nos outros.
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
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.02"));
        }
    },

    OURO {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.05"));
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

    /** Credito guardado para a proxima compra, em centavos. */
    public abstract BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    /** Por padrao o nivel paga frete. */
    public boolean isentaFrete() {
        return false;
    }

    /** Por padrao o nivel nao ganha brinde. */
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
