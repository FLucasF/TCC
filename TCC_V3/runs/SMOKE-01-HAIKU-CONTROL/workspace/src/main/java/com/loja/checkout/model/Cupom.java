package com.loja.checkout.model;

public enum Cupom {
    BEMVINDO10,
    MENOS50,
    FRETEGRATIS,
    LEVE3PAGUE2;

    public static Cupom fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Cupom.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
