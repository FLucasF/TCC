package com.loja.checkout.web;

/** Quando não dá para calcular, o serviço devolve só o código do problema. */
public record RespostaErro(String erro) {
}
