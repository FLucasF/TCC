package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal credito(BigDecimal subtotal) {
            return BigDecimal.ZERO;
        }
    },
    PRATA {
        @Override
        public BigDecimal credito(BigDecimal subtotal) {
            return subtotal.multiply(new BigDecimal("0.02"));
        }
    },
    OURO {
        @Override
        public BigDecimal credito(BigDecimal subtotal) {
            return subtotal.multiply(new BigDecimal("0.05"));
        }

        @Override
        public BigDecimal frete(BigDecimal valorFrete) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500")) > 0;
        }
    };

    public abstract BigDecimal credito(BigDecimal subtotal);

    public BigDecimal frete(BigDecimal valorFrete) {
        return valorFrete;
    }

    public boolean temBrinde(BigDecimal subtotal) {
        return false;
    }
}
