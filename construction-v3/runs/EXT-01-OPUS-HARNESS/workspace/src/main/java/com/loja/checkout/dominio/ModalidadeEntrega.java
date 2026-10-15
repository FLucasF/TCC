package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Cada modalidade sabe quanto cobra, em quantos dias entrega e que pedidos atende.
 * Modalidade nova entra como uma constante nova, com esses tres pontos no corpo dela.
 */
public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return porKg("12.00", "2.00", pesoKg);
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return porKg("25.00", "4.50", pesoKg);
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.ZERO;
        }
    },

    MOTOBOY(0) {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.centavos(new BigDecimal("18.00"));
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(PESO_MAXIMO_MOTOBOY) <= 0;
        }
    };

    private static final BigDecimal PESO_MAXIMO_MOTOBOY = new BigDecimal("5");

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal frete(BigDecimal pesoKg);

    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }

    static BigDecimal porKg(String fixo, String porKg, BigDecimal pesoKg) {
        return Dinheiro.centavos(new BigDecimal(fixo).add(new BigDecimal(porKg).multiply(pesoKg)));
    }
}
