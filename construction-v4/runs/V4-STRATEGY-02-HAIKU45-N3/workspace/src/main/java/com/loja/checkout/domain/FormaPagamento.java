package com.loja.checkout.domain;

public enum FormaPagamento {
    PIX("PIX"),
    CARTAO("CARTAO"),
    BOLETO("BOLETO");

    private final String codigo;

    FormaPagamento(String codigo) {
        this.codigo = codigo;
    }

    public static FormaPagamento de(String codigo) {
        for (FormaPagamento f : values()) {
            if (f.codigo.equals(codigo)) {
                return f;
            }
        }
        return null;
    }
}
