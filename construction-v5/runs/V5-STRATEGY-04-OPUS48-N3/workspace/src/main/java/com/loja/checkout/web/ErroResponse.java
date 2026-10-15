package com.loja.checkout.web;

/**
 * O que o serviço devolve quando recusa o pedido: só o código do problema.
 */
public record ErroResponse(String erro) {
}
