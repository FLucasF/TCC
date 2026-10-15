package br.com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao do marketing. Para publicar um cupom novo basta criar uma classe que
 * implemente esta interface e anota-la com @Component.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em letras maiusculas. */
    String codigo();

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Quanto o cupom abate, em centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
