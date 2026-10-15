package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma forma de entrega oferecida pela loja.
 *
 * <p>Para colocar uma transportadora nova no ar basta criar uma classe que implemente
 * esta interface e anota-la com {@code @Component}: o catalogo se monta sozinho.
 */
public interface ModalidadeEntrega {

    /** Codigo usado pelo site, em letras maiusculas (ex.: {@code EXPRESSA}). */
    String codigo();

    /** Valor do frete para o pedido, ja arredondado em centavos. */
    BigDecimal frete(Pedido pedido);

    /** Prazo prometido, em dias. */
    int prazoEntregaDias(Pedido pedido);

    /** {@code false} quando a modalidade existe mas nao atende este pedido. */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
