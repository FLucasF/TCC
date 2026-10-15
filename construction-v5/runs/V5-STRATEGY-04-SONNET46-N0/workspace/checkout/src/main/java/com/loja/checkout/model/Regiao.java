package com.loja.checkout.model;

public enum Regiao {
    SUDESTE(0.01),
    SUL(0.01),
    CENTRO_OESTE(0.015),
    NORTE(0.025),
    NORDESTE(0.02);

    public final double taxaSeguro;

    Regiao(double taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }
}
