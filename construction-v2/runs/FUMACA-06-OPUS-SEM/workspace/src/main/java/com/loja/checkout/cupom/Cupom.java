package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma promocao. Para lancar um cupom novo, basta criar uma implementacao
 * anotada com {@code @Component}: ela entra no catalogo sozinha.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em maiusculas. */
    String codigo();

    /** Valor do desconto, arredondado para centavos. */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }
}
