package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return round(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg)));
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return round(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg)));
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },

    MOTOBOY(0) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public int getPrazoDias() {
        return prazoDias;
    }

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);

    public abstract boolean disponivel(BigDecimal pesoKg);

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
