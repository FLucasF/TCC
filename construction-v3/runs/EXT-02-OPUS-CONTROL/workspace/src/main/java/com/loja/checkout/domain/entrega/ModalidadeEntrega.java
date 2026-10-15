package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.pedido.Pedido;
import java.math.BigDecimal;

/**
 * Uma forma de entrega da loja. Para acrescentar uma transportadora nova basta criar
 * uma implementacao anotada com @Component: ela entra sozinha no catalogo.
 */
public interface ModalidadeEntrega {

    /** Codigo usado na chamada do site, ex.: "EXPRESSA". */
    String codigo();

    /** Frete cobrado, arredondado para centavos. */
    BigDecimal frete(Pedido pedido);

    /** Prazo prometido, em dias. */
    int prazoEntregaDias(Pedido pedido);

    /** Limitacoes da modalidade (peso, valor, etc.). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
