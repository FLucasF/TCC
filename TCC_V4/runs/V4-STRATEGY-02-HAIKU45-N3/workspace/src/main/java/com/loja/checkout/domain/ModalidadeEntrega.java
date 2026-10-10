package com.loja.checkout.domain;

public enum ModalidadeEntrega {
    ECONOMICA("ECONOMICA"),
    EXPRESSA("EXPRESSA"),
    RETIRADA_LOJA("RETIRADA_LOJA"),
    MOTOBOY("MOTOBOY");

    private final String codigo;

    ModalidadeEntrega(String codigo) {
        this.codigo = codigo;
    }

    public static ModalidadeEntrega de(String codigo) {
        for (ModalidadeEntrega m : values()) {
            if (m.codigo.equals(codigo)) {
                return m;
            }
        }
        return null;
    }
}
