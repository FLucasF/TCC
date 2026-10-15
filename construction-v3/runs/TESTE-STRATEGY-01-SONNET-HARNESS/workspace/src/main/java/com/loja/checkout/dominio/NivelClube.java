package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal percentualCredito() {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean isentoFrete() {
            return false;
        }

        @Override
        public boolean elegivelBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.02");
        }

        @Override
        public boolean isentoFrete() {
            return false;
        }

        @Override
        public boolean elegivelBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO {
        @Override
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.05");
        }

        @Override
        public boolean isentoFrete() {
            return true;
        }

        @Override
        public boolean elegivelBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal percentualCredito();

    public abstract boolean isentoFrete();

    public abstract boolean elegivelBrinde(BigDecimal subtotalProdutos);
}
