package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum NivelClube {
    BRONZE {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return new BigDecimal("0.00");
        }

        @Override
        public boolean temFreteGratis() {
            return false;
        }

        @Override
        public boolean ganharBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },
    PRATA {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos
                .multiply(new BigDecimal("0.02"))
                .setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean temFreteGratis() {
            return false;
        }

        @Override
        public boolean ganharBrinde(BigDecimal subtotalProdutos) {
            return false;
        }
    },
    OURO {
        @Override
        public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
            return subtotalProdutos
                .multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean temFreteGratis() {
            return true;
        }

        @Override
        public boolean ganharBrinde(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
        }
    };

    public abstract BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    public abstract boolean temFreteGratis();
    public abstract boolean ganharBrinde(BigDecimal subtotalProdutos);
}
