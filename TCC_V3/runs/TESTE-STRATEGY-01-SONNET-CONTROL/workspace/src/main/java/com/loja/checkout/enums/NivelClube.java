package com.loja.checkout.enums;

import java.math.BigDecimal;

/**
 * Cada nível do clube define suas próprias vantagens.
 * Novos níveis entram como uma nova constante do enum.
 */
public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal percentualCredito() {
            return BigDecimal.ZERO;
        }
    },

    PRATA {
        @Override
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.02");
        }
    },

    OURO {
        private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

        @Override
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.05");
        }

        @Override
        public boolean freteGratis() {
            return true;
        }

        @Override
        public boolean elegivelBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
        }
    };

    public abstract BigDecimal percentualCredito();

    public boolean freteGratis() {
        return false;
    }

    public boolean elegivelBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
