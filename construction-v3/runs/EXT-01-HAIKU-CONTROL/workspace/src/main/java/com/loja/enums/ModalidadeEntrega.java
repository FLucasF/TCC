package com.loja.enums;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7, Double.MAX_VALUE),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2, Double.MAX_VALUE),
    RETIRADA_LOJA(new BigDecimal("0.00"), new BigDecimal("0.00"), 1, Double.MAX_VALUE),
    MOTOBOY(new BigDecimal("18.00"), new BigDecimal("0.00"), 0, 5.0);

    private final BigDecimal valorBase;
    private final BigDecimal valorPorKg;
    private final Integer prazoDias;
    private final Double pesoMaximoKg;

    ModalidadeEntrega(BigDecimal valorBase, BigDecimal valorPorKg, Integer prazoDias, Double pesoMaximoKg) {
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

    public Double getPesoMaximoKg() {
        return pesoMaximoKg;
    }
}
