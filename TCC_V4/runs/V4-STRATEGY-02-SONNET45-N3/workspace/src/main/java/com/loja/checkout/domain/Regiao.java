package com.loja.checkout.domain;

public enum Regiao {
    SUDESTE(1.0),
    SUL(1.0),
    CENTRO_OESTE(1.5),
    NORTE(2.5),
    NORDESTE(2.0);

    private final double porcentagemSeguro;

    Regiao(double porcentagemSeguro) {
        this.porcentagemSeguro = porcentagemSeguro;
    }

    public double getPorcentagemSeguro() {
        return porcentagemSeguro;
    }
}
