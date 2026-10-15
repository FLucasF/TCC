package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Formas de entrega. Cada modalidade carrega o proprio jeito de cobrar,
 * o proprio prazo e a propria limitacao; por padrao atende qualquer pedido.
 */
public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return porKg("12.00", "2.00", pesoKg);
        }

        @Override
        public int prazoDias() {
            return 7;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return porKg("25.00", "4.50", pesoKg);
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
        private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.centavos(new BigDecimal("18.00"));
        }

        @Override
        public int prazoDias() {
            return 0;
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(PESO_MAXIMO) <= 0;
        }
    };

    public abstract BigDecimal frete(BigDecimal pesoKg);

    public abstract int prazoDias();

    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    static BigDecimal porKg(String fixo, String porKg, BigDecimal pesoKg) {
        return Dinheiro.centavos(new BigDecimal(fixo).add(new BigDecimal(porKg).multiply(pesoKg)));
    }
}
