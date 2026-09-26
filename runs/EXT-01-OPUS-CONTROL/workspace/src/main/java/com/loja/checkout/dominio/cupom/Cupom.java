package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao do marketing. Cupom novo e so uma implementacao anotada com
 * @Component; o codigo e sempre em letras maiusculas.
 */
public interface Cupom {

    String codigo();

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Valor do desconto, em centavos ja arredondados. */
    BigDecimal desconto(ContextoCupom contexto);
}
