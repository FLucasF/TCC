package com.loja.checkout.api;

import java.util.List;

/** O pedido de resumo enviado pelo site no fechamento da compra. */
public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas) {
}
