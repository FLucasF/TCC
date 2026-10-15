package com.loja.checkout.api.dto;

import java.util.List;

/** Os dados da compra, como o site envia para /checkout/resumo. */
public record ResumoCompraRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
