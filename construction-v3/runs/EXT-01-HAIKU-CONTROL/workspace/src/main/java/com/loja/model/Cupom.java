package com.loja.model;

import java.math.BigDecimal;

public enum Cupom {
    BEMVINDO10("BEMVINDO10"),
    MENOS50("MENOS50"),
    FRETEGRATIS("FRETEGRATIS"),
    LEVE3PAGUE2("LEVE3PAGUE2");

    private final String codigo;

    Cupom(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static Cupom fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Cupom cupom : Cupom.values()) {
            if (cupom.codigo.equals(codigo)) {
                return cupom;
            }
        }
        return null;
    }
}
