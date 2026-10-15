package com.loja.checkout.model;

import java.math.BigDecimal;

public enum Modalidade {
    ECONOMICA(7) {
        @Override public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("12").add(new BigDecimal("2").multiply(pesoKg));
        }
    },
    EXPRESSA(2) {
        @Override public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("25").add(new BigDecimal("4.5").multiply(pesoKg));
        }
    },
    RETIRADA_LOJA(1) {
        @Override public BigDecimal frete(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(0) {
        @Override public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("18");
        }
        @Override public boolean suporta(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    public final int prazoDias;

    Modalidade(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal frete(BigDecimal pesoKg);

    public boolean suporta(BigDecimal pesoKg) {
        return true;
    }
}
