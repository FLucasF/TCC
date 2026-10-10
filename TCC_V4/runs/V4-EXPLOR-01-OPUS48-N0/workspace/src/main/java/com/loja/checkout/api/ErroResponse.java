package com.loja.checkout.api;

/** Resposta de recusa: só o código do problema, ex.: {@code { "erro": "PEDIDO_INVALIDO" }}. */
public record ErroResponse(String erro) {
}
