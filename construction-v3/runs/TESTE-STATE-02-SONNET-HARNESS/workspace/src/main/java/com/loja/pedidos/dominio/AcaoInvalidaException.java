package com.loja.pedidos.dominio;

public class AcaoInvalidaException extends RuntimeException {

    public AcaoInvalidaException(String acao) {
        super("Ação inválida: " + acao);
    }
}
