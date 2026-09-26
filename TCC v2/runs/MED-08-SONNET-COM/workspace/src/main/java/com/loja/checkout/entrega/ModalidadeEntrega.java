package com.loja.checkout.entrega;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA(7) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoTotalKg));
        }
    },
    EXPRESSA(2) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoTotalKg));
        }
    },
    RETIRADA_LOJA(1) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(0) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
            return new BigDecimal("18.00");
        }

        @Override
        public boolean disponivel(BigDecimal pesoTotalKg) {
            return pesoTotalKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final int prazoDias;

    ModalidadeEntrega(int prazoDias) {
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    public int prazoDias() {
        return prazoDias;
    }
}
