package com.loja.checkout.model;

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

    public static Regiao de(String nome) {
        if (nome == null) {
            return null;
        }
        try {
            return Regiao.valueOf(nome.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
