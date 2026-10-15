package com.loja.checkout.enums;

public enum ModalidadeEntrega {
    ECONOMICA(7),
    EXPRESSA(2),
    RETIRADA_LOJA(1),
    MOTOBOY(0);

    private final int prazo;

    ModalidadeEntrega(int prazo) {
        this.prazo = prazo;
    }

    public int getPrazo() {
        return prazo;
    }
}
