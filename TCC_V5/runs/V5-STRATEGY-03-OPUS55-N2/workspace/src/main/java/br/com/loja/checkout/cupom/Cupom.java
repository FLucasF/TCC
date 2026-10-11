package br.com.loja.checkout.cupom;

import java.math.BigDecimal;

/** Um cupom promocional. Para incluir um novo, basta criar outro componente que implemente esta interface. */
public interface Cupom {

    String codigo();

    BigDecimal desconto(ContextoCupom contexto);

    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
