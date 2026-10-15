package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg)));
        }

        @Override
        public int prazo() {
            return 7;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return arredondar(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg)));
        }

        @Override
        public int prazo() {
            return 2;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public int prazo() {
            return 1;
        }
    },

    MOTOBOY {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public int prazo() {
            return 0;
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);

    public abstract int prazo();

    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
