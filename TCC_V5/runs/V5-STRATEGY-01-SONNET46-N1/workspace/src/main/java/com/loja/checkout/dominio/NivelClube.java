package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum NivelClube {

    BRONZE {
        @Override
        public boolean isFreteGratis() { return false; }

        @Override
        public BigDecimal calcularCredito(BigDecimal subtotal) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) { return false; }
    },

    PRATA {
        @Override
        public boolean isFreteGratis() { return false; }

        @Override
        public BigDecimal calcularCredito(BigDecimal subtotal) {
            return subtotal.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) { return false; }
    },

    OURO {
        @Override
        public boolean isFreteGratis() { return true; }

        @Override
        public BigDecimal calcularCredito(BigDecimal subtotal) {
            return subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500")) > 0;
        }
    };

    public abstract boolean isFreteGratis();

    public abstract BigDecimal calcularCredito(BigDecimal subtotal);

    public abstract boolean temBrinde(BigDecimal subtotal);
}
