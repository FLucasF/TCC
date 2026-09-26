package com.loja.checkout.modelo;

import java.math.BigDecimal;
import java.util.List;

public enum Cupom {
    BEMVINDO10 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<?> itens) {
            return subtotal.multiply(new BigDecimal("0.10"));
        }

        @Override
        public boolean podeAplicar(BigDecimal subtotal, List<?> itens) {
            return true;
        }
    },
    MENOS50 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<?> itens) {
            return new BigDecimal("50.00");
        }

        @Override
        public boolean podeAplicar(BigDecimal subtotal, List<?> itens) {
            return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
        }
    },
    FRETEGRATIS {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<?> itens) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean podeAplicar(BigDecimal subtotal, List<?> itens) {
            return true;
        }
    },
    LEVE3PAGUE2 {
        @Override
        public BigDecimal calcularDesconto(BigDecimal subtotal, List<?> itens) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean podeAplicar(BigDecimal subtotal, List<?> itens) {
            return true;
        }
    };

    public abstract BigDecimal calcularDesconto(BigDecimal subtotal, List<?> itens);
    public abstract boolean podeAplicar(BigDecimal subtotal, List<?> itens);

    public static Cupom fromString(String codigo) {
        if (codigo == null) {
            return null;
        }
        try {
            return Cupom.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
