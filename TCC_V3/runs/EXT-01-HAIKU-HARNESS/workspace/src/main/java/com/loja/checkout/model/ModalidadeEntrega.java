package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return basico.add(porKg.multiply(pesoKg));
        }
    },
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return basico.add(porKg.multiply(pesoKg));
        }
    },
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return BigDecimal.ZERO;
        }
    },
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0) {
        @Override
        public BigDecimal calcularFrete(BigDecimal pesoKg) {
            return basico;
        }
    };

    protected final BigDecimal basico;
    protected final BigDecimal porKg;
    public final int prazoDias;

    ModalidadeEntrega(BigDecimal basico, BigDecimal porKg, int prazoDias) {
        this.basico = basico;
        this.porKg = porKg;
        this.prazoDias = prazoDias;
    }

    public abstract BigDecimal calcularFrete(BigDecimal pesoKg);

    public boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
