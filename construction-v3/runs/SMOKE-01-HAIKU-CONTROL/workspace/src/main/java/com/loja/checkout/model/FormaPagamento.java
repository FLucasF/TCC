package com.loja.checkout.model;

public enum FormaPagamento {
    PIX,
    CARTAO,
    BOLETO;

    public static FormaPagamento fromString(String value) {
        try {
            return FormaPagamento.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
