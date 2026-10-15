package com.loja.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum NivelClube {

    BRONZE {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isFreteGratis() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    PRATA {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isFreteGratis() {
            return false;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },

    OURO {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isFreteGratis() {
            return true;
        }

        @Override
        public boolean temBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    public abstract boolean isFreteGratis();

    public abstract boolean temBrinde(BigDecimal subtotalProdutos);
}
