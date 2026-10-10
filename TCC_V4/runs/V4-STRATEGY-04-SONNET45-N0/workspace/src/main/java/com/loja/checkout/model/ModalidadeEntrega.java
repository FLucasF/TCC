package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1, null),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0, new BigDecimal("5.00"));

    private final BigDecimal valorBase;
    private final BigDecimal valorPorKg;
    private final int prazoDias;
    private final BigDecimal pesoMaximo;

    ModalidadeEntrega(BigDecimal valorBase, BigDecimal valorPorKg, int prazoDias, BigDecimal pesoMaximo) {
        this.valorBase = valorBase;
        this.valorPorKg = valorPorKg;
        this.prazoDias = prazoDias;
        this.pesoMaximo = pesoMaximo;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public BigDecimal getValorPorKg() {
        return valorPorKg;
    }

    public int getPrazoDias() {
        return prazoDias;
    }

    public BigDecimal getPesoMaximo() {
        return pesoMaximo;
    }

    public boolean aceitaPeso(BigDecimal peso) {
        return pesoMaximo == null || peso.compareTo(pesoMaximo) <= 0;
    }
}
