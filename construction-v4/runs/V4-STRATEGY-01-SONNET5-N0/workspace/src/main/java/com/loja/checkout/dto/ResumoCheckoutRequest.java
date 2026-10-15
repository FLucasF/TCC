package com.loja.checkout.dto;

import java.util.List;

public record ResumoCheckoutRequest(
        List<ItemPedidoRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao
) {
}
