package com.loja.pedidos.dominio;

import java.util.List;

/**
 * O que uma acao faz: para qual situacao o pedido vai e o que mais acontece com ele.
 *
 * @param destino situacao em que o pedido fica depois da acao
 * @param efeitos reembolso, volta de estoque, agendamento de coleta, etc.
 */
public record Transicao(Situacao destino, List<Efeito> efeitos) {

    public Transicao(Situacao destino, Efeito... efeitos) {
        this(destino, List.of(efeitos));
    }

    /** Move o pedido para o destino e aplica os efeitos combinados. */
    public void executar(Pedido pedido) {
        pedido.mover(destino);
        efeitos.forEach(efeito -> efeito.aplicar(pedido));
    }
}
