package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma forma de entrega. Para oferecer uma transportadora nova basta criar uma
 * implementacao anotada com @Component: ela entra sozinha no catalogo.
 */
public interface ModalidadeEntrega {

    /** Codigo que o site envia em modalidadeEntrega. */
    String codigo();

    /** Prazo prometido ao cliente, em dias. */
    int prazoEntregaDias();

    /** Frete cobrado por esta modalidade, ja arredondado em centavos. */
    BigDecimal frete(Pedido pedido);

    /** Se esta modalidade atende o pedido (peso, dimensoes, regiao...). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
