package com.loja.checkout.api;

import java.util.List;

/** Dados da compra que o site envia para /checkout/resumo. */
public record CheckoutRequest(
        List<ItemCarrinho> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
