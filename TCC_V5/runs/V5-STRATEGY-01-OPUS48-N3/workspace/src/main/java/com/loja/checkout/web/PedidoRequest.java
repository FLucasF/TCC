package com.loja.checkout.web;

import java.util.List;

/**
 * A compra como o site envia para /checkout/resumo. Os campos de código chegam
 * como texto (não enum) de propósito: um valor desconhecido precisa virar o
 * nosso código de erro, e não um erro de desserialização.
 */
public record PedidoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
