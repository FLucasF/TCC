package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ModalidadeEntrega {
    ECONOMICA {
        @Override public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pesoTotalKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
        @Override public int prazoDias() { return 7; }
        @Override public boolean disponivel(BigDecimal pesoTotalKg) { return true; }
    },
    EXPRESSA {
        @Override public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pesoTotalKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
        @Override public int prazoDias() { return 2; }
        @Override public boolean disponivel(BigDecimal pesoTotalKg) { return true; }
    },
    RETIRADA_LOJA {
        @Override public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        @Override public int prazoDias() { return 1; }
        @Override public boolean disponivel(BigDecimal pesoTotalKg) { return true; }
    },
    MOTOBOY {
        private static final BigDecimal LIMITE_PESO = new BigDecimal("5");

        @Override public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("18.00");
        }
        @Override public int prazoDias() { return 0; }
        @Override public boolean disponivel(BigDecimal pesoTotalKg) {
            return pesoTotalKg.compareTo(LIMITE_PESO) <= 0;
        }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotalKg);
    public abstract int prazoDias();
    public abstract boolean disponivel(BigDecimal pesoTotalKg);
}
