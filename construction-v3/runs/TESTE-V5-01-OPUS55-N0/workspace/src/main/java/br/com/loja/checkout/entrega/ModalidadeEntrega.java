package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;

import java.math.BigDecimal;

/**
 * Uma opção de entrega. Para incluir uma transportadora nova, basta criar um {@code @Component}
 * que implemente esta interface (ou estenda {@link FretePorPeso}).
 */
public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    /** Valor do frete para o pedido, já em centavos. */
    BigDecimal frete(Carrinho carrinho);

    /** Se a opção consegue levar este pedido (limite de peso, por exemplo). */
    default boolean atende(Carrinho carrinho) {
        return true;
    }
}
