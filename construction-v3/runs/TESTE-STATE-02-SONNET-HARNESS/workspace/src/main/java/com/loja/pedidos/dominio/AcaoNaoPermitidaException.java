package com.loja.pedidos.dominio;

public class AcaoNaoPermitidaException extends RuntimeException {

    public AcaoNaoPermitidaException(String pedidoId, Acao acao, Situacao situacaoAtual) {
        super("Ação " + acao + " não permitida para o pedido " + pedidoId + " na situação " + situacaoAtual);
    }
}
