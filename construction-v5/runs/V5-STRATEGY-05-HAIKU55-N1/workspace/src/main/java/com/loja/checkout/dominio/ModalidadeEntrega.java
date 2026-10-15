package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA(7) {
        public BigDecimal valorFrete(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }
    },
    EXPRESSA(2) {
        public BigDecimal valorFrete(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }
    },
    RETIRADA_LOJA(1) {
        public BigDecimal valorFrete(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(0) {
        public BigDecimal valorFrete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal valorFrete(BigDecimal pesoKg);

    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }
}
