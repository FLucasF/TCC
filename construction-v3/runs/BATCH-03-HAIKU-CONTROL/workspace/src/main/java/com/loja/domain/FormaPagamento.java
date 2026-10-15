package com.loja.domain;

import java.util.Optional;

public enum FormaPagamento {
    PIX,
    CARTAO,
    BOLETO;

    public static Optional<FormaPagamento> fromString(String value) {
        try {
            return Optional.of(FormaPagamento.valueOf(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
