package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(12.00, 2.00, 7),
    EXPRESSA(25.00, 4.50, 2),
    RETIRADA_LOJA(0.00, 0.00, 1),
    MOTOBOY(18.00, 0.00, 0);

    private final BigDecimal taxaBase;
    private final BigDecimal taxaPorKg;
    private final int prazo;

    ModalidadeEntrega(double taxaBase, double taxaPorKg, int prazo) {
        this.taxaBase = BigDecimal.valueOf(taxaBase);
        this.taxaPorKg = BigDecimal.valueOf(taxaPorKg);
        this.prazo = prazo;
    }

    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        if (this == RETIRADA_LOJA) {
            return BigDecimal.ZERO;
        }
        return taxaBase.add(taxaPorKg.multiply(pesoTotal));
    }

    public boolean isDisponivelPara(BigDecimal pesoTotal) {
        if (this == MOTOBOY) {
            return pesoTotal.compareTo(BigDecimal.valueOf(5)) <= 0;
        }
        return true;
    }

    public int getPrazo() {
        return prazo;
    }
}
