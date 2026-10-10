package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1, null),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0, new BigDecimal("5.00"));

    private final BigDecimal valorBase;
    private final BigDecimal valorPorKg;
    private final Integer prazoDias;
    private final BigDecimal pesoMaximoKg;

    ModalidadeEntrega(BigDecimal valorBase, BigDecimal valorPorKg, Integer prazoDias, BigDecimal pesoMaximoKg) {
        this.valorBase = valorBase;
        this.valorPorKg = valorPorKg;
        this.prazoDias = prazoDias;
        this.pesoMaximoKg = pesoMaximoKg;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public BigDecimal getValorPorKg() {
        return valorPorKg;
    }

    public Integer getPrazoDias() {
        return prazoDias;
    }

    public BigDecimal getPesoMaximoKg() {
        return pesoMaximoKg;
    }
}
