package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Para adicionar uma transportadora nova basta criar uma
 * classe que implemente esta interface e anota-la com {@code @Component}.
 */
public interface ModalidadeEntrega {

    /** Codigo enviado pelo site, ex.: "EXPRESSA". */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoEntregaDias();

    /** Valor do frete para este pedido, arredondado para centavos. */
    BigDecimal calcularFrete(Pedido pedido);

    /** Se esta opcao atende o pedido (limite de peso, regiao, etc.). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
