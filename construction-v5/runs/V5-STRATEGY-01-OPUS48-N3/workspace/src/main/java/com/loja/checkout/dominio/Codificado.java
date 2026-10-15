package com.loja.checkout.dominio;

/**
 * Tudo que o cliente escolhe por um código (modalidade de entrega, cupom, nível
 * do clube, forma de pagamento) sabe dizer o seu código. Permite que a busca por
 * código seja a mesma para todas as dimensões.
 */
public interface Codificado {
    String codigo();
}
