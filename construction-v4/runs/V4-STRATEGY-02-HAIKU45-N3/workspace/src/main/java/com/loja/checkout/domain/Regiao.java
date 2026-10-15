package com.loja.checkout.domain;

public enum Regiao {
    SUDESTE("SUDESTE"),
    SUL("SUL"),
    CENTRO_OESTE("CENTRO_OESTE"),
    NORTE("NORTE"),
    NORDESTE("NORDESTE");

    private final String codigo;

    Regiao(String codigo) {
        this.codigo = codigo;
    }

    public static Regiao de(String codigo) {
        for (Regiao r : values()) {
            if (r.codigo.equals(codigo)) {
                return r;
            }
        }
        return null;
    }
}
