package com.loja.checkout.api;

import java.util.List;

/** Dados da compra enviados pelo site para calcular o resumo. */
public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas) {
}
