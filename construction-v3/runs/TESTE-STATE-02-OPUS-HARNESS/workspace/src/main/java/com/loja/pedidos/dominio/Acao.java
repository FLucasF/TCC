package com.loja.pedidos.dominio;

import java.util.Optional;

public enum Acao {
    PAGAR, SEPARAR, ENVIAR, ENTREGAR, CANCELAR, DEVOLVER;

    public static Optional<Acao> deNome(String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        for (Acao acao : values()) {
            if (acao.name().equals(nome)) {
                return Optional.of(acao);
            }
        }
        return Optional.empty();
    }
}
