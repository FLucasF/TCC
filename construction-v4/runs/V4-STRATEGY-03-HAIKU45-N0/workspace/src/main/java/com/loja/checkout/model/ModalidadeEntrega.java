package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0);

    private final BigDecimal baseValue;
    private final BigDecimal perKgValue;
    private final int prazo;

    ModalidadeEntrega(BigDecimal baseValue, BigDecimal perKgValue, int prazo) {
        this.baseValue = baseValue;
        this.perKgValue = perKgValue;
        this.prazo = prazo;
    }

    public BigDecimal getBaseValue() {
        return baseValue;
    }

    public BigDecimal getPerKgValue() {
        return perKgValue;
    }

    public int getPrazo() {
        return prazo;
    }
}
