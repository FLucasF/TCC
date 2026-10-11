package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum NivelClube {
    BRONZE {
        @Override
        public BigDecimal calcularCredito(BigDecimal sub) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public boolean isFreteGratis() {
            return false;
        }

        @Override
        public boolean isDireitoABrinde(BigDecimal sub) {
            return false;
        }
    },
    PRATA {
        @Override
        public BigDecimal calcularCredito(BigDecimal sub) {
            return sub.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isFreteGratis() {
            return false;
        }

        @Override
        public boolean isDireitoABrinde(BigDecimal sub) {
            return false;
        }
    },
    OURO {
        @Override
        public BigDecimal calcularCredito(BigDecimal sub) {
            return sub.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isFreteGratis() {
            return true;
        }

        @Override
        public boolean isDireitoABrinde(BigDecimal sub) {
            return sub.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    public abstract boolean isFreteGratis();
    public abstract boolean isDireitoABrinde(BigDecimal subtotalProdutos);
}
