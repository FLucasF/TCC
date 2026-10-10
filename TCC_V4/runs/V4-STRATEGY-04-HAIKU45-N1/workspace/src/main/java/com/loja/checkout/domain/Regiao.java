package com.loja.checkout.domain;

public enum Regiao {
    SUDESTE(0.01),
    SUL(0.01),
    CENTRO_OESTE(0.015),
    NORTE(0.025),
    NORDESTE(0.02);

    private final Double taxaSeguro;

    Regiao(Double taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }

    public Double getTaxaSeguro() {
        return taxaSeguro;
    }

    public static Regiao parse(String valor) {
        if (valor == null) return null;
        try {
            return Regiao.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
