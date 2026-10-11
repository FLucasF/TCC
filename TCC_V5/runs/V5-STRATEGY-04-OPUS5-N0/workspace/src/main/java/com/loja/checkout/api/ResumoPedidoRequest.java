package com.loja.checkout.api;

import java.util.List;

/** Os dados da compra, como o site envia para /checkout/resumo. */
public record ResumoPedidoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
