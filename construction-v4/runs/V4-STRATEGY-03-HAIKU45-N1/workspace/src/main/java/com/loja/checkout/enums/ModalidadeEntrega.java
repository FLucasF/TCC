package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(12.00, 2.00, 7),
    EXPRESSA(25.00, 4.50, 2),
    RETIRADA_LOJA(0.00, 0.00, 1),
    MOTOBOY(18.00, 0.00, 0);

    private final BigDecimal valorBase;
    private final BigDecimal valorPorKg;
    private final int prazo;

    ModalidadeEntrega(double valorBase, double valorPorKg, int prazo) {
        this.valorBase = BigDecimal.valueOf(valorBase);
        this.valorPorKg = BigDecimal.valueOf(valorPorKg);
        this.prazo = prazo;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public BigDecimal getValorPorKg() {
        return valorPorKg;
    }

    public int getPrazo() {
        return prazo;
    }

    public boolean contemPesoLimite(BigDecimal pesoTotal) {
        if (this == MOTOBOY) {
            return pesoTotal.compareTo(BigDecimal.valueOf(5)) <= 0;
        }
        return true;
    }
}
