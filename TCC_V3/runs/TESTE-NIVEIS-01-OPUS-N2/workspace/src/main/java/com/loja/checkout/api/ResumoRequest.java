package com.loja.checkout.api;

import java.util.List;

/** Os dados da compra, como o site envia. */
public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
