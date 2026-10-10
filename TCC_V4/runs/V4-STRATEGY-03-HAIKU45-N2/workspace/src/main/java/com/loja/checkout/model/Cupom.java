package com.loja.checkout.model;

import java.math.BigDecimal;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos) {
            return subtotalProdutos.multiply(new BigDecimal("0.10"));
        }
    },
    MENOS50 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos) {
            return subtotalProdutos.compareTo(new BigDecimal("300")) >= 0;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos) {
            return new BigDecimal("50.00");
        }
    },
    FRETEGRATIS {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean ehFreteGratis() {
            return true;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public boolean aplicavel(BigDecimal subtotalProdutos) {
            return true;
        }

        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotalProdutos) {
            return BigDecimal.ZERO;
        }
    };

    public abstract boolean aplicavel(BigDecimal subtotalProdutos);
    public abstract BigDecimal calcularDesconto(BigDecimal subtotalProdutos);

    public boolean ehFreteGratis() {
        return false;
    }
}
