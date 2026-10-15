package br.com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Um cupom de desconto. Para criar uma promoção nova, crie uma classe
 * {@code @Component} que implemente esta interface.
 */
public interface Cupom {

    /** Código digitado pelo cliente, sempre em maiúsculas. */
    String codigo();

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto já arredondado para centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
