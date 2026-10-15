package com.loja.checkout.web;

/**
 * Resposta quando o pedido é recusado: só o código do problema.
 */
public record ErroResposta(String erro) {
}
