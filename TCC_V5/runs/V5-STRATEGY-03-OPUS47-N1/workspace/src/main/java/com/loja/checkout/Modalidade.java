package com.loja.checkout;

import java.math.BigDecimal;

public enum Modalidade {
    ECONOMICA {
        @Override public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }
        @Override public int prazoDias() { return 7; }
        @Override public boolean atende(BigDecimal pesoKg) { return true; }
    },
    EXPRESSA {
        @Override public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }
        @Override public int prazoDias() { return 2; }
        @Override public boolean atende(BigDecimal pesoKg) { return true; }
    },
    RETIRADA_LOJA {
        @Override public BigDecimal frete(BigDecimal pesoKg) { return BigDecimal.ZERO; }
        @Override public int prazoDias() { return 1; }
        @Override public boolean atende(BigDecimal pesoKg) { return true; }
    },
    MOTOBOY {
        @Override public BigDecimal frete(BigDecimal pesoKg) { return new BigDecimal("18.00"); }
        @Override public int prazoDias() { return 0; }
        @Override public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    public abstract BigDecimal frete(BigDecimal pesoKg);
    public abstract int prazoDias();
    public abstract boolean atende(BigDecimal pesoKg);
}
