package com.loja.domain;

public enum Cupom {
    BEMVINDO10,
    MENOS50,
    FRETEGRATIS,
    LEVE3PAGUE2;

    public static Cupom fromCodigo(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return null;
        }
        try {
            return Cupom.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
