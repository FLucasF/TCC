package com.loja.model;

public enum Regiao {
    SUDESTE(0.01),
    SUL(0.01),
    CENTRO_OESTE(0.015),
    NORTE(0.025),
    NORDESTE(0.02);

    private final double seguroPercentual;

    Regiao(double seguroPercentual) {
        this.seguroPercentual = seguroPercentual;
    }

    public double getSeguroPercentual() {
        return seguroPercentual;
    }

    public static Regiao fromCodigo(String codigo) {
        if (codigo == null) return null;
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
