package com.lojaroupas.model;

import java.math.BigDecimal;

public enum Regiao {
    SUDESTE("SUDESTE", new BigDecimal("0.01")),
    SUL("SUL", new BigDecimal("0.01")),
    CENTRO_OESTE("CENTRO_OESTE", new BigDecimal("0.015")),
    NORTE("NORTE", new BigDecimal("0.025")),
    NORDESTE("NORDESTE", new BigDecimal("0.02"));

    private final String codigo;
    private final BigDecimal percentualSeguro;

    Regiao(String codigo, BigDecimal percentualSeguro) {
        this.codigo = codigo;
        this.percentualSeguro = percentualSeguro;
    }

    public static Regiao fromCodigo(String codigo) {
        for (Regiao r : values()) {
            if (r.codigo.equals(codigo)) {
                return r;
            }
        }
        return null;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getPercentualSeguro() {
        return percentualSeguro;
    }

}
