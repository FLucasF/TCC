package com.loja.checkout.domain;

public enum Regiao {
    SUDESTE("SUDESTE", 0.01),
    SUL("SUL", 0.01),
    CENTRO_OESTE("CENTRO_OESTE", 0.015),
    NORTE("NORTE", 0.025),
    NORDESTE("NORDESTE", 0.02);

    private final String codigo;
    private final double percentualSeguro;

    Regiao(String codigo, double percentualSeguro) {
        this.codigo = codigo;
        this.percentualSeguro = percentualSeguro;
    }

    public String getCodigo() {
        return codigo;
    }

    public double getPercentualSeguro() {
        return percentualSeguro;
    }

    public static Regiao fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Regiao r : values()) {
            if (r.codigo.equals(codigo)) {
                return r;
            }
        }
        return null;
    }
}
