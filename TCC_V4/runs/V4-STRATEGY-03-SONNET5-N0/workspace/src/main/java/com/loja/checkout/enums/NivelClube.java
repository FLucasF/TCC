package com.loja.checkout.enums;

import java.math.BigDecimal;

/**
 * Cada nível carrega seu próprio conjunto de vantagens.
 * Um novo nível entra como uma nova constante, sem tocar no resto do código.
 */
public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal percentualCredito() {
            return BigDecimal.ZERO;
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
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.02");
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
        private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500");

        @Override
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.05");
        }

        @Override
        public boolean isentaFrete() {
            return true;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
        }
    };

    public abstract BigDecimal percentualCredito();

    public abstract boolean isentaFrete();

    public abstract boolean temBrinde(BigDecimal subtotalProdutos);
}
