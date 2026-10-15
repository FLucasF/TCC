package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pesoTotalKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isDisponivel(BigDecimal pesoTotalKg) {
            return true;
        }
    },

    EXPRESSA(2) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pesoTotalKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        @Override
        public boolean isDisponivel(BigDecimal pesoTotalKg) {
            return true;
        }
    },

    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("0.00");
        }

        @Override
        public boolean isDisponivel(BigDecimal pesoTotalKg) {
            return true;
        }
    },

    MOTOBOY(0) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public boolean isDisponivel(BigDecimal pesoTotalKg) {
            return pesoTotalKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public int getPrazoDias() {
        return prazoDias;
    }

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    public abstract boolean isDisponivel(BigDecimal pesoTotalKg);
}
