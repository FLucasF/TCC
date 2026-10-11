package com.loja.checkout.resumo;

import java.util.List;

/** A compra como o site envia. */
public record PedidoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
