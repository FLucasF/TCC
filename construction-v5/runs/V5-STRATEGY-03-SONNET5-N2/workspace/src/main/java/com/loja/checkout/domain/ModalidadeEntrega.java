package com.loja.checkout.domain;

import com.loja.checkout.service.CheckoutException;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }

        @Override
        public int prazoDias() {
            return 7;
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }

        @Override
        public int prazoDias() {
            return 2;
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }

        @Override
        public int prazoDias() {
            return 1;
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },

    MOTOBOY {
        private static final BigDecimal LIMITE_KG = new BigDecimal("5");

        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(LIMITE_KG) <= 0;
        }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);

    public abstract int prazoDias();

    public abstract boolean disponivel(BigDecimal pesoKg);

    public static ModalidadeEntrega from(String codigo) {
        if (codigo == null) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        try {
            return valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
    }
}
