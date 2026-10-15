package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao da loja. Promocao nova e uma classe nova implementando esta
 * interface, marcada com @Component.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em maiusculas (ex.: BEMVINDO10). */
    String codigo();

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto do cupom, arredondado para centavos. */
    BigDecimal calcularDesconto(ContextoCupom contexto);
}
