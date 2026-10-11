package com.loja.checkout.web;

import java.util.List;

/**
 * Corpo que o site envia para /checkout/resumo. Os campos de escolha
 * (modalidade, cupom, pagamento, nível, região) chegam como texto e são
 * resolvidos para o caso correspondente no serviço; assim um valor
 * desconhecido vira o erro certo em vez de quebrar a desserialização.
 */
public record CheckoutRequest(
        List<ItemPedido> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
