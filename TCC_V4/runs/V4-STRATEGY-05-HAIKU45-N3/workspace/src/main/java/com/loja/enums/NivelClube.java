package com.loja.enums;

public enum NivelClube {
    BRONZE,
    PRATA,
    OURO;

    public static NivelClube fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return NivelClube.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
