package com.loja.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotal) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public boolean freteGratis() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotal) {
            return subtotal.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean freteGratis() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) {
            return false;
        }
    },

    OURO {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotal) {
            return subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean freteGratis() {
            return true;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotal) {
            return subtotal.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal calcularCredito(BigDecimal subtotal);

    public abstract boolean freteGratis();

    public abstract boolean temBrinde(BigDecimal subtotal);
}
