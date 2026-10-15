package com.loja.checkout.api;

/**
 * Resposta quando o pedido e recusado: apenas o codigo do problema.
 * Ex.: { "erro": "PEDIDO_INVALIDO" }.
 */
public record ErroResponse(String erro) {
}
