package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega implements Entrega {

    ECONOMICA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pesoTotalKg)));
        }

        @Override
        public int prazoEntregaDias() {
            return 7;
        }

        @Override
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return true;
        }
    },

    EXPRESSA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pesoTotalKg)));
        }

        @Override
        public int prazoEntregaDias() {
            return 2;
        }

        @Override
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return true;
        }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(BigDecimal.ZERO);
        }

        @Override
        public int prazoEntregaDias() {
            return 1;
        }

        @Override
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return true;
        }
    },

    MOTOBOY {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return Dinheiro.arredondar(new BigDecimal("18.00"));
        }

        @Override
        public int prazoEntregaDias() {
            return 0;
        }

        @Override
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return pesoTotalKg.compareTo(PESO_MAXIMO_MOTOBOY_KG) <= 0;
        }
    };

    private static final BigDecimal PESO_MAXIMO_MOTOBOY_KG = new BigDecimal("5");
}
