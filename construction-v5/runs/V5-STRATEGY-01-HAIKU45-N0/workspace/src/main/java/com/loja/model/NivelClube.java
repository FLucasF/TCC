package com.loja.model;

public enum NivelClube {
    BRONZE(0.00, false),
    PRATA(0.02, false),
    OURO(0.05, true);

    private final double creditoPercentual;
    private final boolean fretGratis;

    NivelClube(double creditoPercentual, boolean fretGratis) {
        this.creditoPercentual = creditoPercentual;
        this.fretGratis = fretGratis;
    }

    public double getCreditoPercentual() {
        return creditoPercentual;
    }

    public boolean isFretGratis() {
        return fretGratis;
    }

    public static NivelClube fromCodigo(String codigo) {
        if (codigo == null) return null;
        try {
            return NivelClube.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
