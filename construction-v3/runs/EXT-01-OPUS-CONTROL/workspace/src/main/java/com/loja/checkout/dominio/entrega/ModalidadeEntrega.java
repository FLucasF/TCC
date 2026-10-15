package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega da loja. Para entrar com uma transportadora nova basta
 * criar uma implementacao anotada com @Component: ela e registrada sozinha.
 */
public interface ModalidadeEntrega {

    /** Codigo usado pelo site, ex.: EXPRESSA. */
    String codigo();

    /** Prazo prometido ao cliente, em dias. */
    int prazoDias();

    /** Se a modalidade atende um pedido com esse peso. */
    default boolean atende(BigDecimal pesoKg) {
        return true;
    }

    /** Valor do frete em centavos ja arredondados. */
    BigDecimal custo(BigDecimal pesoKg);
}
