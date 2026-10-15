package com.loja.checkout;

import java.math.BigDecimal;

enum ModalidadeEntrega {

    ECONOMICA {
        BigDecimal frete(BigDecimal peso) {
            return Dinheiro.centavos(new BigDecimal("12").add(new BigDecimal("2").multiply(peso)));
        }

        int prazoDias() {
            return 7;
        }
    },

    EXPRESSA {
        BigDecimal frete(BigDecimal peso) {
            return Dinheiro.centavos(new BigDecimal("25").add(new BigDecimal("4.5").multiply(peso)));
        }

        int prazoDias() {
            return 2;
        }
    },

    RETIRADA_LOJA {
        BigDecimal frete(BigDecimal peso) {
            return Dinheiro.ZERO;
        }

        int prazoDias() {
            return 1;
        }
    },

    MOTOBOY {
        BigDecimal frete(BigDecimal peso) {
            return Dinheiro.centavos(new BigDecimal("18"));
        }

        int prazoDias() {
            return 0;
        }

        boolean disponivel(BigDecimal peso) {
            return peso.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    abstract BigDecimal frete(BigDecimal peso);

    abstract int prazoDias();

    boolean disponivel(BigDecimal peso) {
        return true;
    }
}
