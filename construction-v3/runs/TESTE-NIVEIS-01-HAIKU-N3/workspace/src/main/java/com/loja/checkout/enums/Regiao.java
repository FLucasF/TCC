package com.loja.checkout.enums;

public enum Regiao {
    SUDESTE(0.01),
    SUL(0.01),
    CENTRO_OESTE(0.015),
    NORTE(0.025),
    NORDESTE(0.02);

    private final double percentualSeguro;

    Regiao(double percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public double getPercentualSeguro() {
        return percentualSeguro;
    }
}
