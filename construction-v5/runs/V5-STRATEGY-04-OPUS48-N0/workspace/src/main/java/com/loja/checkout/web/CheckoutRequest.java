package com.loja.checkout.web;

import java.util.List;

/**
 * Dados da compra enviados pelo site para {@code /checkout/resumo}.
 *
 * <p>Os campos que representam opções ({@code modalidadeEntrega}, {@code cupom},
 * {@code formaPagamento}, {@code nivelClube}, {@code regiao}) chegam como texto e são
 * validados no serviço, para que valores inexistentes virem o código de erro certo
 * em vez de um erro de desserialização.
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
