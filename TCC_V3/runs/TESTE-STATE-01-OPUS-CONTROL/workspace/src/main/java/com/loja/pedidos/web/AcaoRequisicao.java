package com.loja.pedidos.web;

/**
 * Corpo de POST /pedidos/{id}/acoes.
 *
 * <p>A ação chega como texto de propósito: um nome desconhecido precisa virar
 * ACAO_INVALIDA, e só depois de conferir que o pedido existe.
 */
public record AcaoRequisicao(String acao) {
}
