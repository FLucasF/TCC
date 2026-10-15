package br.com.loja.pedidos.pedido;

import java.time.Instant;
import java.util.List;

import br.com.loja.pedidos.checkout.Resumo;

public record Pedido(long id, String cpf, SituacaoPedido situacao, Instant criadoEm, Resumo resumo,
        List<ItemPedido> itens, String codigoRastreio, String idTransacao) {
}
