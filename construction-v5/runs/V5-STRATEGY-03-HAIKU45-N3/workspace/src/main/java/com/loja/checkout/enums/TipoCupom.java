package com.loja.checkout.enums;

public enum TipoCupom {
    BEMVINDO10,
    MENOS50,
    FRETEGRATIS,
    LEVE3PAGUE2;

    public static TipoCupom fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TipoCupom.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
