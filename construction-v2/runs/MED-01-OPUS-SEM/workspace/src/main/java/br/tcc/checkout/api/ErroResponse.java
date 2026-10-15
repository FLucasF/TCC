package br.tcc.checkout.api;

/** Corpo devolvido em qualquer erro de negócio: {@code {"erro": "CODIGO"}}. */
public record ErroResponse(String erro) {
}
