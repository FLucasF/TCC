package com.loja.checkout.dominio;

import java.util.List;

/** Pedido como chega do site, antes de ser validado. */
public record PedidoRequisicao(List<ItemRequisicao> itens,
                               String modalidadeEntrega,
                               String cupom,
                               String formaPagamento,
                               Integer parcelas) {
}
