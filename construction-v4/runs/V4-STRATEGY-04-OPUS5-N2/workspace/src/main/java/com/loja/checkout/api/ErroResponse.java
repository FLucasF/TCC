package com.loja.checkout.api;

/** A resposta de um pedido recusado: so o codigo do problema. */
public record ErroResponse(String erro) {
}
