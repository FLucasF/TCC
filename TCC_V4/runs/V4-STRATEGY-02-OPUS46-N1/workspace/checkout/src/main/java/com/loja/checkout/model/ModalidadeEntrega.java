package com.loja.checkout.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("12.00")
                    .add(new BigDecimal("2.00").multiply(pesoKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
    },
    EXPRESSA(2) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("25.00")
                    .add(new BigDecimal("4.50").multiply(pesoKg))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
    },
    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return BigDecimal.ZERO.setScale(2);
        }
    },
    MOTOBOY(0) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public boolean isDisponivel(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);

    public int getPrazoDias() {
        return prazoDias;
    }

    public boolean isDisponivel(BigDecimal pesoKg) {
        return true;
    }
}
