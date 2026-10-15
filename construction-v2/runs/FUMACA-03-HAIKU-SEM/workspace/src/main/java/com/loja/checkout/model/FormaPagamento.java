package com.loja.checkout.model;

public enum FormaPagamento {
    PIX("PIX"),
    CARTAO("CARTAO"),
    BOLETO("BOLETO");

    private final String codigo;

    FormaPagamento(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static FormaPagamento fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (FormaPagamento f : FormaPagamento.values()) {
            if (f.codigo.equals(codigo)) {
                return f;
            }
        }
        return null;
    }
}
