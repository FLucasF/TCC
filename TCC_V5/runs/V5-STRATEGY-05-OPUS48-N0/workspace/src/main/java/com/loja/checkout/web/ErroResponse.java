package com.loja.checkout.web;

/**
 * Resposta devolvida quando o pedido é recusado: só o código do problema.
 *
 * @param erro código do problema (ver {@link com.loja.checkout.domain.CodigoErro})
 */
public record ErroResponse(String erro) {
}
