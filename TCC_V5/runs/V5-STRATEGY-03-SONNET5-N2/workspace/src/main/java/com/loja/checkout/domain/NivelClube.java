package com.loja.checkout.domain;

import com.loja.checkout.service.CheckoutException;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal percentualCredito() {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean pagaFrete() {
            return true;
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
        public boolean pagaFrete() {
            return true;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO {
        private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

        @Override
        public BigDecimal percentualCredito() {
            return new BigDecimal("0.05");
        }

        @Override
        public boolean pagaFrete() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
        }
    };

    public abstract BigDecimal percentualCredito();

    public abstract boolean pagaFrete();

    public abstract boolean temBrinde(BigDecimal subtotalProdutos);

    public static NivelClube from(String codigo) {
        if (codigo == null) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        try {
            return valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
    }
}
