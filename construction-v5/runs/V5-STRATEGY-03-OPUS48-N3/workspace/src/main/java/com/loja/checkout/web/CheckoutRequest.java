package com.loja.checkout.web;

import java.util.List;

/**
 * Dados da compra, como o site envia. Os campos de opção chegam como texto para
 * o serviço poder dizer exatamente qual é inválido. {@code cupom} e
 * {@code parcelas} podem não vir.
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
