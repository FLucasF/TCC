package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cada modalidade de entrega define o seu frete, o seu prazo e as suas limitações.
 * Modalidade nova entra como uma constante nova, sem mexer nas outras.
 */
public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.arredondar(new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg)));
        }

        @Override
        public int prazoDias() {
            return 7;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.arredondar(new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg)));
        }

        @Override
        public int prazoDias() {
            return 2;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.ZERO;
        }

        @Override
        public int prazoDias() {
            return 1;
        }
    },

    MOTOBOY {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.arredondar(new BigDecimal("18.00"));
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    public abstract BigDecimal frete(BigDecimal pesoKg);

    public abstract int prazoDias();

    public boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
