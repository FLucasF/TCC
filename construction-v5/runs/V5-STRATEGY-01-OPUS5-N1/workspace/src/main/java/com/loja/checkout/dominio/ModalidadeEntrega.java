package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Formas de entrega. Cada modalidade define o proprio custo, o proprio prazo
 * e os pedidos que consegue atender.
 */
public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return porKg("12.00", "2.00", pesoKg);
        }

        @Override
        public int prazoEntregaDias() {
            return 7;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return porKg("25.00", "4.50", pesoKg);
        }

        @Override
        public int prazoEntregaDias() {
            return 2;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.centavos(BigDecimal.ZERO);
        }

        @Override
        public int prazoEntregaDias() {
            return 1;
        }
    },

    MOTOBOY {
        private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

        @Override
        public BigDecimal frete(BigDecimal pesoKg) {
            return Dinheiro.centavos(new BigDecimal("18.00"));
        }

        @Override
        public int prazoEntregaDias() {
            return 0;
        }

        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
        }
    };

    public abstract BigDecimal frete(BigDecimal pesoKg);

    public abstract int prazoEntregaDias();

    public boolean atende(BigDecimal pesoKg) {
        return true;
    }

    static BigDecimal porKg(String fixo, String porKg, BigDecimal pesoKg) {
        return Dinheiro.centavos(new BigDecimal(fixo).add(new BigDecimal(porKg).multiply(pesoKg)));
    }
}
