package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Para criar uma nova transportadora basta escrever uma classe
 * que implemente esta interface e anota-la com {@code @Component}: ela entra sozinha
 * no catalogo, sem mexer em mais nada.
 */
public interface ModalidadeEntrega {

    /** Codigo usado pelo site, ex.: "EXPRESSA". */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoDias(Pedido pedido);

    /** Diz se esta opcao atende o pedido (peso, regiao, etc.). */
    default boolean atende(Pedido pedido) {
        return true;
    }

    /** Valor do frete, ja arredondado para centavos. */
    BigDecimal calcularFrete(Pedido pedido);
}
