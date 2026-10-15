package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ModalidadeEntrega {
    ECONOMICA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pesoKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public int prazoEntregaDias() { return 7; }

        @Override
        public boolean isDisponivel(BigDecimal pesoKg) { return true; }
    },

    EXPRESSA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pesoKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public int prazoEntregaDias() { return 2; }

        @Override
        public boolean isDisponivel(BigDecimal pesoKg) { return true; }
    },

    RETIRADA_LOJA {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return BigDecimal.ZERO.setScale(2);
        }

        @Override
        public int prazoEntregaDias() { return 1; }

        @Override
        public boolean isDisponivel(BigDecimal pesoKg) { return true; }
    },

    MOTOBOY {
        private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public int prazoEntregaDias() { return 0; }

        @Override
        public boolean isDisponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(PESO_MAXIMO) <= 0;
        }
    };

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);

    public abstract int prazoEntregaDias();

    public abstract boolean isDisponivel(BigDecimal pesoKg);
}
