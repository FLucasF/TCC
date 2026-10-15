package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum Modalidade {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("12").add(new BigDecimal("2").multiply(pesoKg));
        }
    },
    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("25").add(new BigDecimal("4.5").multiply(pesoKg));
        }
    },
    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(0) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("18");
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    Modalidade(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public int prazoDias() {
        return prazoDias;
    }

    public abstract BigDecimal frete(BigDecimal pesoKg);

    public boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
