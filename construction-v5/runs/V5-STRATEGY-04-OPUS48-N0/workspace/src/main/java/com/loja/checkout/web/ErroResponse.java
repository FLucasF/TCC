package com.loja.checkout.web;

/**
 * Resposta de erro: só o código do problema, como {@code {"erro": "CODIGO"}}.
 */
public record ErroResponse(String erro) {
}
