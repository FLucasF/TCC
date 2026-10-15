package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Para oferecer uma transportadora nova, basta criar uma
 * implementacao desta interface anotada com @Component: ela entra no catalogo
 * automaticamente, sem alterar o calculo do resumo.
 */
public interface ModalidadeEntrega {

    /** Codigo enviado pelo site (ex.: EXPRESSA). */
    String codigo();

    /** Diz se esta opcao atende um pedido com o peso informado. */
    default boolean atende(BigDecimal pesoKgPedido) {
        return true;
    }

    /** Valor do frete para o peso informado, arredondado para centavos. */
    BigDecimal frete(BigDecimal pesoKgPedido);

    /** Prazo de entrega em dias. */
    int prazoDias();
}
