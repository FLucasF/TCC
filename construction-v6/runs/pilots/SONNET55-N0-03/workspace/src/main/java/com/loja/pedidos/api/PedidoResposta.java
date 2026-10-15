package com.loja.pedidos.api;

import java.time.Instant;
import java.util.List;

import com.loja.pedidos.dominio.ItemPedido;
import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.Resumo;

public record PedidoResposta(
        String id,
        String situacao,
        Instant criadoEm,
        Resumo resumo,
        List<ItemPedido> itens,
        String codigoRastreio) {

    public static PedidoResposta de(Pedido p) {
        return new PedidoResposta(p.id(), p.situacao().name(), p.criadoEm(), p.resumo(), p.itens(), p.codigoRastreio());
    }
}
