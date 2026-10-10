package com.loja.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0);

    private final BigDecimal baseCost;
    private final BigDecimal costPerKg;
    private final int prazo;

    ModalidadeEntrega(BigDecimal baseCost, BigDecimal costPerKg, int prazo) {
        this.baseCost = baseCost;
        this.costPerKg = costPerKg;
        this.prazo = prazo;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public BigDecimal getCostPerKg() {
        return costPerKg;
    }

    public int getPrazo() {
        return prazo;
    }

    public BigDecimal calculateCost(BigDecimal totalWeightKg) {
        return baseCost.add(costPerKg.multiply(totalWeightKg));
    }

    public boolean isMototoy() {
        return this == MOTOBOY;
    }

    public boolean isFreeForOuro() {
        return this != RETIRADA_LOJA;
    }
}
