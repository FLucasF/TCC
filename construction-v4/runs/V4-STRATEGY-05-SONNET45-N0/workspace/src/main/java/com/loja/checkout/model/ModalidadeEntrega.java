package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1, null),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0, new BigDecimal("5.00"));

    private final BigDecimal valorBase;
    private final BigDecimal valorPorKg;
    private final int prazoEntregaDias;
    private final BigDecimal pesoMaximoKg;

    ModalidadeEntrega(BigDecimal valorBase, BigDecimal valorPorKg, int prazoEntregaDias, BigDecimal pesoMaximoKg) {
        this.valorBase = valorBase;
        this.valorPorKg = valorPorKg;
        this.prazoEntregaDias = prazoEntregaDias;
        this.pesoMaximoKg = pesoMaximoKg;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public BigDecimal getValorPorKg() {
        return valorPorKg;
    }

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public BigDecimal getPesoMaximoKg() {
        return pesoMaximoKg;
    }

    public boolean aceitaPeso(BigDecimal pesoTotal) {
        return pesoMaximoKg == null || pesoTotal.compareTo(pesoMaximoKg) <= 0;
    }
}
