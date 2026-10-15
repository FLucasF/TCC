package com.loja.model;

public enum FormaPagamento {
    PIX,
    CARTAO,
    BOLETO;

    public static FormaPagamento fromCodigo(String codigo) {
        if (codigo == null) return null;
        try {
            return FormaPagamento.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
