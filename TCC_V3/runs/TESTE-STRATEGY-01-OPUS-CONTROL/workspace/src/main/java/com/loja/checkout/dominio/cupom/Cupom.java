package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao. Para criar um cupom novo basta implementar esta interface
 * e anotar a classe com {@code @Component}.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em maiusculas. */
    String codigo();

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto concedido, em reais e centavos. */
    BigDecimal calcularDesconto(ContextoCupom contexto);
}
