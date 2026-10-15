package br.com.loja.checkout.web;

/** A resposta de pedido recusado: só o código do problema. */
public record ErroResponse(String erro) {
}
