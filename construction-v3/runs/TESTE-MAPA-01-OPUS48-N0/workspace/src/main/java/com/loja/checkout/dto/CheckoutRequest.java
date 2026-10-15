package com.loja.checkout.dto;

import java.util.List;

/**
 * Dados da compra enviados pelo site. Os campos de domínio chegam como texto
 * (não enum) de propósito: assim um valor desconhecido vira o código de erro
 * correto, na ordem de verificação definida, em vez de um erro de parsing.
 */
public record CheckoutRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao
) {
}
