package com.loja.pedidos.api;

public record RespostaDeErro(String erro) {

    public static RespostaDeErro de(Erro erro) {
        return new RespostaDeErro(erro.name());
    }
}
