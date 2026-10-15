package com.loja.checkout.calculo;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        BigDecimal freteBruto(BigDecimal peso) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(peso));
        }
    },
    EXPRESSA(2) {
        @Override
        BigDecimal freteBruto(BigDecimal peso) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(peso));
        }
    },
    RETIRADA_LOJA(1) {
        @Override
        BigDecimal freteBruto(BigDecimal peso) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(0) {
        @Override
        BigDecimal freteBruto(BigDecimal peso) {
            return new BigDecimal("18.00");
        }

        @Override
        boolean disponivel(BigDecimal peso) {
            return peso.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    abstract BigDecimal freteBruto(BigDecimal peso);

    boolean disponivel(BigDecimal peso) {
        return true;
    }

    int prazoDias() {
        return prazoDias;
    }
}
