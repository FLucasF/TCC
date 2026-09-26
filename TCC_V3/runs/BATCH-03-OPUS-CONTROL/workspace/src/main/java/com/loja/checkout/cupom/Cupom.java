package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao. Para criar uma promocao nova basta implementar esta interface
 * e anotar a classe com {@code @Component}.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em letras maiusculas. */
    String codigo();

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Valor do desconto, arredondado para centavos. */
    BigDecimal calcularDesconto(ContextoCupom contexto);
}
