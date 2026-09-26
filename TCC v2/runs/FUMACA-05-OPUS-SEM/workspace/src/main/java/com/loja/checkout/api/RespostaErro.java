package com.loja.checkout.api;

/** Corpo devolvido nos erros 400: {"erro": "CODIGO"}. */
public record RespostaErro(String erro) {
}
