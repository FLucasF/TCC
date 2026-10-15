package com.loja.checkout.enums;

public enum ModalidadeEntrega {
    ECONOMICA(12.00, 2.00, 7),
    EXPRESSA(25.00, 4.50, 2),
    RETIRADA_LOJA(0.00, 0.00, 1),
    MOTOBOY(18.00, 0.00, 0);

    private final Double taxaBase;
    private final Double taxaPorKg;
    private final Integer prazo;

    ModalidadeEntrega(Double taxaBase, Double taxaPorKg, Integer prazo) {
        this.taxaBase = taxaBase;
        this.taxaPorKg = taxaPorKg;
        this.prazo = prazo;
    }

    public Double getTaxaBase() {
        return taxaBase;
    }

    public Double getTaxaPorKg() {
        return taxaPorKg;
    }

    public Integer getPrazo() {
        return prazo;
    }
}
