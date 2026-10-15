package br.com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Um cupom de desconto. Para criar um novo, basta implementar esta interface
 * em uma classe anotada com {@code @Component}.
 */
public interface Cupom {

    /** Código exatamente como o cliente digita (maiúsculas). */
    String codigo();

    BigDecimal desconto(ContextoCupom contexto);

    /** Se o pedido cumpre a condição do cupom (valor mínimo, por exemplo). */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
