package com.loja.domain;

public enum Modalidade {
    ECONOMICA(12.0, 2.0, 7),
    EXPRESSA(25.0, 4.5, 2),
    RETIRADA_LOJA(0.0, 0.0, 1),
    MOTOBOY(18.0, 0.0, 0);

    private final Double baseValue;
    private final Double perKg;
    private final Integer prazo;

    Modalidade(Double baseValue, Double perKg, Integer prazo) {
        this.baseValue = baseValue;
        this.perKg = perKg;
        this.prazo = prazo;
    }

    public Double getBaseValue() {
        return baseValue;
    }

    public Double getPerKg() {
        return perKg;
    }

    public Integer getPrazo() {
        return prazo;
    }
}
