package com.loja.resumo.calculo;

import java.math.BigDecimal;

public enum Entrega {

    ECONOMICA(7) {
        @Override
        BigDecimal frete(BigDecimal peso) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(peso));
        }
    },
    EXPRESSA(2) {
        @Override
        BigDecimal frete(BigDecimal peso) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(peso));
        }
    },
    RETIRADA_LOJA(1) {
        @Override
        BigDecimal frete(BigDecimal peso) {
            return Dinheiro.ZERO;
        }
    },
    MOTOBOY(0) {
        @Override
        BigDecimal frete(BigDecimal peso) {
            return new BigDecimal("18.00");
        }

        @Override
        boolean atende(BigDecimal peso) {
            return peso.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    Entrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    int prazoDias() {
        return prazoDias;
    }

    abstract BigDecimal frete(BigDecimal peso);

    boolean atende(BigDecimal peso) {
        return true;
    }
}
