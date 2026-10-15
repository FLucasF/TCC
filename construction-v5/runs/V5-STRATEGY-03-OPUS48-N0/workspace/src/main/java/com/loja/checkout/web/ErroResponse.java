package com.loja.checkout.web;

/**
 * Resposta quando o pedido e recusado: {@code { "erro": "CODIGO" }}.
 */
public record ErroResponse(String erro) {
}
