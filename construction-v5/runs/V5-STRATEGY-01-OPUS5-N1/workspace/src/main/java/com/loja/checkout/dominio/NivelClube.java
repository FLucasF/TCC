package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Niveis do clube da loja. Cada nivel define o credito que gera, se paga
 * frete e se leva brinde.
 */
public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.centavos(BigDecimal.ZERO);
        }
    },

    PRATA {
        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.02"));
        }
    },

    OURO {
        private static final BigDecimal PRODUTOS_PARA_BRINDE = new BigDecimal("500.00");

        @Override
        public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
            return Dinheiro.percentual(subtotalProdutos, new BigDecimal("0.05"));
        }

        @Override
        public BigDecimal frete(BigDecimal freteDaModalidade) {
            return Dinheiro.centavos(BigDecimal.ZERO);
        }

        @Override
        public boolean brinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(PRODUTOS_PARA_BRINDE) > 0;
        }
    };

    public abstract BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    public BigDecimal frete(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
