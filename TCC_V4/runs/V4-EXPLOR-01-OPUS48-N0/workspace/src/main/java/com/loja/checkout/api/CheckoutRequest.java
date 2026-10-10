package com.loja.checkout.api;

import java.util.List;

/**
 * O pedido que o site envia para {@code /checkout/resumo}.
 *
 * @param itens             produtos do carrinho
 * @param modalidadeEntrega como o cliente quer receber
 * @param cupom             código do cupom (pode não vir)
 * @param formaPagamento    como vai pagar
 * @param parcelas          em quantas vezes (pode não vir; nesse caso é 1)
 * @param nivelClube        nível do cliente no clube
 * @param regiao            onde o cliente mora
 */
public record CheckoutRequest(List<ItemRequest> itens, String modalidadeEntrega, String cupom,
                              String formaPagamento, Integer parcelas, String nivelClube, String regiao) {
}
