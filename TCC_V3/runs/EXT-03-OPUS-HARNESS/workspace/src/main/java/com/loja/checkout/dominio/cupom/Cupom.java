package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao. Cada cupom novo entra como uma implementacao propria,
 * registrada no catalogo como componente.
 */
public interface Cupom {

    String codigo();

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    BigDecimal desconto(ContextoCupom contexto);
}
