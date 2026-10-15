package br.com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Um cupom de desconto. Para criar um novo, basta escrever uma classe que implemente
 * esta interface e anotá-la com {@code @Component}.
 */
public interface Cupom {

    /** Código exato (em maiúsculas) que o cliente digita. */
    String codigo();

    /** Se o pedido cumpre a condição do cupom (ex.: valor mínimo). */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Valor do desconto, já arredondado para centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
