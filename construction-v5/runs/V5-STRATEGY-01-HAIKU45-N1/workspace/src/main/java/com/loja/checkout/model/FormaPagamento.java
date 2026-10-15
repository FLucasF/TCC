package com.loja.checkout.model;

public enum FormaPagamento {
    PIX,
    CARTAO,
    BOLETO;

    public static FormaPagamento de(String nome) {
        if (nome == null) {
            return null;
        }
        try {
            return FormaPagamento.valueOf(nome.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
