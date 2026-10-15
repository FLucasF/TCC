package br.com.loja.checkout.entrega;

import br.com.loja.checkout.carrinho.Carrinho;

import java.math.BigDecimal;

/**
 * Uma opção de entrega. Para cadastrar uma transportadora nova, crie uma classe
 * {@code @Component} que implemente esta interface.
 */
public interface ModalidadeEntrega {

    /** Código enviado pelo site, ex.: {@code EXPRESSA}. */
    String codigo();

    int prazoDias();

    /** Frete já arredondado para centavos. */
    BigDecimal frete(Carrinho carrinho);

    /** Se a opção consegue atender este pedido (limites de peso, etc.). */
    default boolean atende(Carrinho carrinho) {
        return true;
    }
}
