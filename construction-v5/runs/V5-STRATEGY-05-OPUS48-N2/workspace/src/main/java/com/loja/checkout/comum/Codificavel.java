package com.loja.checkout.comum;

/**
 * Tudo que é escolhido por um código enviado pelo site (entrega, cupom, nível
 * do clube, forma de pagamento) sabe se identificar. A escolha entre os casos
 * vira uma busca por código, nunca uma sequência de condições.
 */
public interface Codificavel {

    String codigo();
}
