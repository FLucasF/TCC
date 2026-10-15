package com.loja.checkout.domain;

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
        for (Cupom c : values()) {
            if (c.codigo.equals(codigo)) {
                return c;
            }
        }
        return null;
    }
}
