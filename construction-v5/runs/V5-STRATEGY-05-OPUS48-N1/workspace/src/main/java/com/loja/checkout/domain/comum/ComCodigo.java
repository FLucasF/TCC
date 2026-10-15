package com.loja.checkout.domain.comum;

/**
 * Tudo que o cliente escolhe por um codigo (entrega, cupom, nivel do clube,
 * forma de pagamento) sabe se identificar. E assim que o pedido acha o caso
 * certo: por busca no codigo, nunca por uma sequencia de condicoes.
 */
public interface ComCodigo {

    String codigo();
}
