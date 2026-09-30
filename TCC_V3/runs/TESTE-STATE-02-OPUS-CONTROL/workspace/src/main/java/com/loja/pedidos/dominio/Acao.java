package com.loja.pedidos.dominio;

import java.util.Optional;

/** Acoes que o site (ou a equipe) pode pedir para um pedido. */
public enum Acao {
    PAGAR,
    SEPARAR,
    ENVIAR,
    ENTREGAR,
    CANCELAR,
    DEVOLVER;

    /** Converte o texto recebido na requisicao, vazio quando a acao nao existe. */
    public static Optional<Acao> de(String nome) {
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
