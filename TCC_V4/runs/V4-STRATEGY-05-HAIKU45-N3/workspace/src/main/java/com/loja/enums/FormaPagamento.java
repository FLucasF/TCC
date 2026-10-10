package com.loja.enums;

public enum FormaPagamento {
    PIX,
    CARTAO,
    BOLETO;

    public static FormaPagamento fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return FormaPagamento.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
