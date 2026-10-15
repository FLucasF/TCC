package com.loja.checkout.domain;

public enum NivelClube {
    BRONZE("BRONZE"),
    PRATA("PRATA"),
    OURO("OURO");

    private final String codigo;

    NivelClube(String codigo) {
        this.codigo = codigo;
    }

    public static NivelClube de(String codigo) {
        for (NivelClube n : values()) {
            if (n.codigo.equals(codigo)) {
                return n;
            }
        }
        return null;
    }
}
