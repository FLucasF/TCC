package com.loja.checkout.web;

/**
 * O que o servico devolve quando recusa o pedido: so o codigo do problema.
 */
public record ErroResponse(String erro) {
}
