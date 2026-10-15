package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Para oferecer uma transportadora nova, basta criar uma
 * implementacao anotada com {@code @Component}: ela entra no catalogo sozinha.
 */
public interface ModalidadeEntrega {

    /** Codigo enviado pelo site, ex.: EXPRESSA. */
    String codigo();

    /** Valor do frete, arredondado para centavos. */
    BigDecimal frete(Pedido pedido);

    /** Prazo de entrega em dias. */
    int prazoDias(Pedido pedido);

    /** Se a opcao atende este pedido (peso, regiao, etc.). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
