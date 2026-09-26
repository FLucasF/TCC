package com.loja.checkout.domain;

public enum FormaPagamento {
    PIX("PIX"),
    CARTAO("CARTAO"),
    BOLETO("BOLETO");

    private final String codigo;

    FormaPagamento(String codigo) {
        this.codigo = codigo;
    }

    public static FormaPagamento fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (FormaPagamento forma : values()) {
            if (forma.codigo.equals(codigo)) {
                return forma;
            }
        }
        return null;
    }
}
