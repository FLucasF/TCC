package com.loja.checkout.web;

/** A resposta quando o pedido é recusado: só o código do problema. */
public record ErroResponse(String erro) {
}
