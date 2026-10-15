package com.loja.checkout.contrato;

/** A resposta de um pedido recusado: so' o codigo do problema. */
public record ErroResponse(String erro) {
}
