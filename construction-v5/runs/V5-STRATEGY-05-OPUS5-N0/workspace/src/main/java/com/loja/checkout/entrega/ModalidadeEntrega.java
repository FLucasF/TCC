package com.loja.checkout.entrega;

import com.loja.checkout.domain.Pedido;
import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Para adicionar uma transportadora nova basta criar
 * uma classe que implemente esta interface e marca-la com @Component:
 * ela passa a ser aceita no campo "modalidadeEntrega" automaticamente.
 */
public interface ModalidadeEntrega {

    /** Codigo usado na requisicao, ex.: EXPRESSA. */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoEntregaDias();

    /** Valor do frete, ja arredondado em centavos. */
    BigDecimal calcularFrete(Pedido pedido);

    /** Se a opcao atende este pedido (ex.: limite de peso do motoboy). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
