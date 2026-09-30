package com.loja.pedidos.dominio;

import java.util.Optional;

/** Acoes que a equipe ou o site podem pedir para um pedido. */
public enum Acao {

    PAGAR,
    SEPARAR,
    ENVIAR,
    ENTREGAR,
    CANCELAR,
    DEVOLVER;

    /** Interpreta o texto recebido na requisicao; vazio quando nao corresponde a nenhuma acao. */
    public static Optional<Acao> porNome(String nome) {
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
