package br.com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Um cupom de desconto. Para criar uma promoção nova, basta criar um {@code @Component}
 * que implemente esta interface.
 */
public interface Cupom {

    /** Código exato (em maiúsculas) que o cliente digita. */
    String codigo();

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Valor do desconto, já em centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
