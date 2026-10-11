package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Codificado;

import java.math.BigDecimal;

/** Um cupom de desconto. Para incluir um novo, basta criar outra implementação anotada com @Component. */
public interface Cupom extends Codificado {

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto, já arredondado para centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
