package com.loja.domain;

public enum ModalidadeEntrega {
    ECONOMICA(12.00, 2.00, 7),
    EXPRESSA(25.00, 4.50, 2),
    RETIRADA_LOJA(0.00, 0.00, 1),
    MOTOBOY(18.00, 0.00, 0);

    private final Double taxaBase;
    private final Double taxaPorKg;
    private final Integer prazoDias;

    ModalidadeEntrega(Double taxaBase, Double taxaPorKg, Integer prazoDias) {
        this.taxaBase = taxaBase;
        this.taxaPorKg = taxaPorKg;
        this.prazoDias = prazoDias;
    }

    public Double getTaxaBase() {
        return taxaBase;
    }

    public Double getTaxaPorKg() {
        return taxaPorKg;
    }

    public Integer getPrazoDias() {
        return prazoDias;
    }

    public Double calcularFrete(Double pesoTotalKg) {
        if (this == MOTOBOY) {
            return taxaBase;
        }
        return taxaBase + (pesoTotalKg * taxaPorKg);
    }
}
