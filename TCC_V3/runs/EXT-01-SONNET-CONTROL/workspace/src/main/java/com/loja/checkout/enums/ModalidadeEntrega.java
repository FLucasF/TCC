package com.loja.checkout.enums;

import java.math.BigDecimal;

/**
 * Cada opcao de entrega tem sua propria forma de cobrar, prazo e
 * disponibilidade. Novas transportadoras entram com frequencia, entao cada
 * constante implementa seu proprio calculo em vez de um if/else central.
 */
public enum ModalidadeEntrega {

    ECONOMICA {
        @Override
        public BigDecimal calcularCusto(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }

        @Override
        public int getPrazoDias() {
            return 7;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal calcularCusto(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }

        @Override
        public int getPrazoDias() {
            return 2;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal calcularCusto(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }

        @Override
        public int getPrazoDias() {
            return 1;
        }
    },

    MOTOBOY {
        private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

        @Override
        public BigDecimal calcularCusto(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public int getPrazoDias() {
            return 0;
        }

        @Override
        public boolean disponivelPara(BigDecimal pesoKg) {
            return pesoKg.compareTo(PESO_MAXIMO_KG) <= 0;
        }
    };

    public abstract BigDecimal calcularCusto(BigDecimal pesoKg);

    public abstract int getPrazoDias();

    public boolean disponivelPara(BigDecimal pesoKg) {
        return true;
    }
}
