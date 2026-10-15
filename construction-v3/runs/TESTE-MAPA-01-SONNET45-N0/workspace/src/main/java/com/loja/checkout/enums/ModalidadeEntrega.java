package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1, null),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0, new BigDecimal("5.00"));

    private final BigDecimal taxaBase;
    private final BigDecimal taxaPorKg;
    private final int prazoEntregaDias;
    private final BigDecimal limitePesoKg;

    ModalidadeEntrega(BigDecimal taxaBase, BigDecimal taxaPorKg, int prazoEntregaDias, BigDecimal limitePesoKg) {
        this.taxaBase = taxaBase;
        this.taxaPorKg = taxaPorKg;
        this.prazoEntregaDias = prazoEntregaDias;
        this.limitePesoKg = limitePesoKg;
    }

    public BigDecimal getTaxaBase() {
        return taxaBase;
    }

    public BigDecimal getTaxaPorKg() {
        return taxaPorKg;
    }

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public BigDecimal getLimitePesoKg() {
        return limitePesoKg;
    }
}
