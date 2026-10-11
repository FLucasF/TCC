package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA(new BigDecimal("0.00"), new BigDecimal("0.00"), 1),
    MOTOBOY(new BigDecimal("18.00"), new BigDecimal("0.00"), 0);

    private final BigDecimal taxaBase;
    private final BigDecimal taxaPorKg;
    private final int prazoDias;

    ModalidadeEntrega(BigDecimal taxaBase, BigDecimal taxaPorKg, int prazoDias) {
        this.taxaBase = taxaBase;
        this.taxaPorKg = taxaPorKg;
        this.prazoDias = prazoDias;
    }

    public BigDecimal getTaxaBase() {
        return taxaBase;
    }

    public BigDecimal getTaxaPorKg() {
        return taxaPorKg;
    }

    public int getPrazoDias() {
        return prazoDias;
    }
}
