package com.loja.checkout.calculo;

import java.math.BigDecimal;

public enum Modalidade {

    ECONOMICA {
        BigDecimal fretePara(BigDecimal pesoKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoKg));
        }

        int prazoDias() {
            return 7;
        }
    },

    EXPRESSA {
        BigDecimal fretePara(BigDecimal pesoKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoKg));
        }

        int prazoDias() {
            return 2;
        }
    },

    RETIRADA_LOJA {
        BigDecimal fretePara(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }

        int prazoDias() {
            return 1;
        }
    },

    MOTOBOY {
        BigDecimal fretePara(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        int prazoDias() {
            return 0;
        }

        boolean disponivelPara(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    abstract BigDecimal fretePara(BigDecimal pesoKg);

    abstract int prazoDias();

    boolean disponivelPara(BigDecimal pesoKg) {
        return true;
    }
}
