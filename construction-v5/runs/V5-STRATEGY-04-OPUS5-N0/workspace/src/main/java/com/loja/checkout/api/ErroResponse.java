package com.loja.checkout.api;

/** Resposta quando o pedido é recusado: só o código do problema. */
public record ErroResponse(String erro) {
}
