package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA(new BigDecimal("0.00"), new BigDecimal("0.00"), 1),
    MOTOBOY(new BigDecimal("18.00"), new BigDecimal("0.00"), 0);

    private final BigDecimal custoBases;
    private final BigDecimal custoPorKg;
    private final int prazo;

    ModalidadeEntrega(BigDecimal custoBases, BigDecimal custoPorKg, int prazo) {
        this.custoBases = custoBases;
        this.custoPorKg = custoPorKg;
        this.prazo = prazo;
    }

    public BigDecimal getCustoBases() {
        return custoBases;
    }

    public BigDecimal getCustoPorKg() {
        return custoPorKg;
    }

    public int getPrazo() {
        return prazo;
    }

    public boolean temLimiteKg() {
        return this == MOTOBOY;
    }

    public int getLimiteKg() {
        return this == MOTOBOY ? 5 : Integer.MAX_VALUE;
    }
}
