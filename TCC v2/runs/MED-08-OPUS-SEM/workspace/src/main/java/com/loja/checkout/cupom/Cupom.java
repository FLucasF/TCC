package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao do marketing.
 *
 * <p>Cupom novo e uma classe nova anotada com {@code @Component}; o catalogo se monta sozinho.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em letras maiusculas. */
    String codigo();

    /** {@code false} quando o cupom existe mas o pedido nao cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto em reais, ja arredondado em centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
