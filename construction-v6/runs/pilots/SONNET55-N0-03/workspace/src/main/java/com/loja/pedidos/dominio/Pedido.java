package com.loja.pedidos.dominio;

import java.time.Instant;
import java.util.List;

/** Pedido com todos os dados internos; a API expõe apenas {@link com.loja.pedidos.api.PedidoResposta}. */
public record Pedido(
        String id,
        String cpf,
        String email,
        String cep,
        String regiao,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        String nivelClube,
        SituacaoPedido situacao,
        Instant criadoEm,
        Resumo resumo,
        List<ItemPedido> itens,
        String codigoRastreio,
        String idTransacao) {
}
