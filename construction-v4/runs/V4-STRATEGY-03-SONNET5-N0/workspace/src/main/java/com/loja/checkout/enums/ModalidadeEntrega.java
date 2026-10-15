package com.loja.checkout.enums;

import java.math.BigDecimal;

/**
 * Cada modalidade carrega sua própria regra de frete, prazo e disponibilidade.
 * Novas transportadoras entram como uma nova constante, sem tocar no resto do código.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },
    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },
    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return true;
        }
    },
    MOTOBOY(0) {
        private static final BigDecimal LIMITE_KG = new BigDecimal("5");

        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public boolean disponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(LIMITE_KG) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public int prazoDias() {
        return prazoDias;
    }

    public abstract BigDecimal frete(BigDecimal pesoKg);

    public abstract boolean disponivel(BigDecimal pesoKg);
}
