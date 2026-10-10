package com.loja.enums;

public enum Regiao {
    SUDESTE,
    SUL,
    CENTRO_OESTE,
    NORTE,
    NORDESTE;

    public static Regiao fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Regiao.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
