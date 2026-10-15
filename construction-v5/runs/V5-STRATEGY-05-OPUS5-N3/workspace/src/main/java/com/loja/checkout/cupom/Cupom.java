package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Identificado;
import java.math.BigDecimal;

/**
 * Promoção. Cada código tem sua condição e sua conta de desconto; promoção nova
 * é uma implementação nova.
 */
public interface Cupom extends Identificado {

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto em reais, já em centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
