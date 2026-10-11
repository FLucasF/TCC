package com.loja.checkout.web;

/** O corpo devolvido quando o pedido e recusado: { "erro": "CODIGO" }. */
public record ErroResponse(String erro) {
}
