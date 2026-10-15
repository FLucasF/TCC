package com.loja.checkout.web;

import java.util.List;

/**
 * Dados da compra, como o site envia para /checkout/resumo.
 */
public record CheckoutRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
