package com.loja.enums;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0);

    private final BigDecimal taxa;
    private final BigDecimal valorPorKg;
    private final int prazo;

    ModalidadeEntrega(BigDecimal taxa, BigDecimal valorPorKg, int prazo) {
        this.taxa = taxa;
        this.valorPorKg = valorPorKg;
        this.prazo = prazo;
    }

    public BigDecimal getTaxa() {
        return taxa;
    }

    public BigDecimal getValorPorKg() {
        return valorPorKg;
    }

    public int getPrazo() {
        return prazo;
    }

    public boolean isMotoboy() {
        return this == MOTOBOY;
    }
}
