package com.loja.checkout.cupom;

import com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface Cupom extends Codificado {

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto já arredondado para centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
