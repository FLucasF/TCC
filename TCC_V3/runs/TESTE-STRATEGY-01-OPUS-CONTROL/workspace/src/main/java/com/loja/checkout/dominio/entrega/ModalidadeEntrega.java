package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Para adicionar uma transportadora nova basta criar
 * uma classe que implemente esta interface e anota-la com {@code @Component}.
 */
public interface ModalidadeEntrega {

    /** Codigo enviado pelo site, ex.: EXPRESSA. */
    String codigo();

    /** Prazo prometido, em dias. */
    int prazoDias();

    /** Frete cobrado para o pedido, em reais e centavos. */
    BigDecimal calcularFrete(Pedido pedido);

    /** Se a modalidade atende o pedido (ex.: motoboy tem limite de peso). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
