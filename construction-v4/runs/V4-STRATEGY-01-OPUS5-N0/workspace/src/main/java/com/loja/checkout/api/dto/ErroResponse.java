package com.loja.checkout.api.dto;

/** Resposta quando o pedido e recusado: so o codigo do problema. */
public record ErroResponse(String erro) {
}
