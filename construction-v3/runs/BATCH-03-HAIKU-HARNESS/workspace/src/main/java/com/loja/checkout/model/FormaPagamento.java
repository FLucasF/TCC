package com.loja.checkout.model;

public enum FormaPagamento {
    PIX,
    CARTAO,
    BOLETO;

    public static FormaPagamento fromString(String forma) {
        if (forma == null) {
            return null;
        }
        try {
            return FormaPagamento.valueOf(forma);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
