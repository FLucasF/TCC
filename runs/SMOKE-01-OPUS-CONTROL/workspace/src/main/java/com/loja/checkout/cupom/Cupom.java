package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao do marketing. Cupom novo = uma classe nova anotada com
 * {@code @Component}; o catalogo se encarrega do resto.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em maiusculas. */
    String codigo();

    /** Diz se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Valor abatido, ja arredondado para centavos. */
    BigDecimal calcularDesconto(ContextoCupom contexto);
}
