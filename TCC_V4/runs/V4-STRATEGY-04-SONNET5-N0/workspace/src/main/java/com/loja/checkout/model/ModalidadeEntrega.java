package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }

        @Override
        public int prazoDias() {
            return 7;
        }
    },
    EXPRESSA {
        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }

        @Override
        public int prazoDias() {
            return 2;
        }
    },
    RETIRADA_LOJA {
        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }

        @Override
        public int prazoDias() {
            return 1;
        }
    },
    MOTOBOY {
        @Override
        public BigDecimal custo(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    public abstract BigDecimal custo(BigDecimal pesoKg);

    public abstract int prazoDias();

    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
