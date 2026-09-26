package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA(new BigDecimal("0.00"), new BigDecimal("0.00"), 1),
    MOTOBOY(new BigDecimal("18.00"), new BigDecimal("0.00"), 0);

    private final BigDecimal valorFixo;
    private final BigDecimal valorPorKg;
    private final int prazoDias;

    ModalidadeEntrega(BigDecimal valorFixo, BigDecimal valorPorKg, int prazoDias) {
        this.valorFixo = valorFixo;
        this.valorPorKg = valorPorKg;
        this.prazoDias = prazoDias;
    }

    public BigDecimal getValorFixo() {
        return valorFixo;
    }

    public BigDecimal getValorPorKg() {
        return valorPorKg;
    }

    public int getPrazoDias() {
        return prazoDias;
    }

    public static ModalidadeEntrega fromString(String value) {
        try {
            return ModalidadeEntrega.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
