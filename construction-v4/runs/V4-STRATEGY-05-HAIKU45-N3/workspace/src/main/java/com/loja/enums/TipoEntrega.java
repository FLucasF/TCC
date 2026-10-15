package com.loja.enums;

public enum TipoEntrega {
    ECONOMICA,
    EXPRESSA,
    RETIRADA_LOJA,
    MOTOBOY;

    public static TipoEntrega fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TipoEntrega.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
