package com.loja.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(12.00, 2.00, 7),
    EXPRESSA(25.00, 4.50, 2),
    RETIRADA_LOJA(0.00, 0.00, 1),
    MOTOBOY(18.00, 0.00, 0);

    private final BigDecimal valorBase;
    private final BigDecimal valorPorKg;
    private final Integer prazo;

    ModalidadeEntrega(double valorBase, double valorPorKg, Integer prazo) {
        this.valorBase = BigDecimal.valueOf(valorBase);
        this.valorPorKg = BigDecimal.valueOf(valorPorKg);
        this.prazo = prazo;
    }

    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        if (this == MOTOBOY) {
            return valorBase;
        }
        return valorBase.add(valorPorKg.multiply(pesoTotal));
    }

    public Integer getPrazo() {
        return prazo;
    }

    public boolean ehDisponivel(BigDecimal pesoTotal) {
        if (this == MOTOBOY && pesoTotal.compareTo(BigDecimal.valueOf(5.0)) > 0) {
            return false;
        }
        return true;
    }
}
