package com.loja.checkout.domain;

public enum NivelClube {
    BRONZE, PRATA, OURO;

    public static NivelClube parse(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return NivelClube.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
