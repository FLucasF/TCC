package com.loja.checkout.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1, null),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0, new BigDecimal("5.00"));

    private final BigDecimal valorBase;
    private final BigDecimal valorPorKg;
    private final int prazoDias;
    private final BigDecimal limiteKg;

    ModalidadeEntrega(BigDecimal valorBase, BigDecimal valorPorKg, int prazoDias, BigDecimal limiteKg) {
        this.valorBase = valorBase;
        this.valorPorKg = valorPorKg;
        this.prazoDias = prazoDias;
        this.limiteKg = limiteKg;
    }

    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal frete = valorBase.add(valorPorKg.multiply(pesoTotal));
        return frete.setScale(2, RoundingMode.HALF_EVEN);
    }

    public boolean aceitaPeso(BigDecimal pesoTotal) {
        return limiteKg == null || pesoTotal.compareTo(limiteKg) <= 0;
    }

    public int getPrazoDias() {
        return prazoDias;
    }
}
