package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Codificavel;
import java.math.BigDecimal;

public interface Cupom extends Codificavel {

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(BaseCupom base) {
        return true;
    }

    BigDecimal desconto(BaseCupom base);
}
