package com.loja.checkout.enums;

public enum ModalidadeEntrega {
    ECONOMICA,
    EXPRESSA,
    RETIRADA_LOJA,
    MOTOBOY;

    public static ModalidadeEntrega fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ModalidadeEntrega.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
