package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Catalogavel;
import java.math.BigDecimal;

/**
 * Uma promocao do marketing. Cada cupom novo e uma implementacao desta
 * interface, com sua condicao e sua forma de descontar.
 */
public interface Cupom extends Catalogavel {

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    BigDecimal desconto(ContextoCupom contexto);
}
