package com.loja.checkout.comum;

/**
 * Tudo que o cliente escolhe por um código (entrega, cupom, nível, pagamento)
 * se identifica assim. Permite um lookup único para todas as famílias.
 */
public interface Codificavel {

    String codigo();
}
