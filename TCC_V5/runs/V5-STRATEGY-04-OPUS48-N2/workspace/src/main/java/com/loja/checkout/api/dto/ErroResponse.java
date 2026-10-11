package com.loja.checkout.api.dto;

/** A resposta quando o pedido é recusado: só o código do problema. */
public record ErroResponse(String erro) {
}
