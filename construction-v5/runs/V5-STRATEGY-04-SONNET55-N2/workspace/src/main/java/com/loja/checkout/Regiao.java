package com.loja.checkout;

public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final String taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }

    public String taxaSeguro() {
        return taxaSeguro;
    }
}
