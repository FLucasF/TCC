package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Para cadastrar uma transportadora nova basta criar uma
 * implementacao anotada com {@code @Component}: ela entra no catalogo sozinha.
 */
public interface ModalidadeEntrega {

    /** Codigo enviado pelo site, em maiusculas (ex.: EXPRESSA). */
    String codigo();

    /** Prazo prometido, em dias. */
    int prazoEntregaDias();

    /** Custo do frete para o pedido, em centavos arredondados. */
    BigDecimal frete(Pedido pedido);

    /** Se a opcao atende este pedido (peso, valor, etc.). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
