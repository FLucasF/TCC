package com.lojaroupas.enums;

public enum Regiao {
    SUDESTE(0.01),
    SUL(0.01),
    CENTRO_OESTE(0.015),
    NORTE(0.025),
    NORDESTE(0.02);

    private final Double taxa;

    Regiao(Double taxa) {
        this.taxa = taxa;
    }

    public Double getTaxa() {
        return taxa;
    }

    public Double calcularSeguro(Double subtotal) {
        return subtotal * taxa;
    }

    public static Regiao fromString(String value) {
        try {
            return Regiao.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
