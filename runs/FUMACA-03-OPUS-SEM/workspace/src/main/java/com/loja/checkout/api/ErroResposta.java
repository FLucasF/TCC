package com.loja.checkout.api;

/** Corpo devolvido nos erros de negocio: {"erro": "CODIGO"}. */
public record ErroResposta(String erro) {
}
