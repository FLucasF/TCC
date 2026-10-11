package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Codificavel;
import java.math.BigDecimal;

/** Uma promoção. Promoção nova = classe nova com @Component. */
public interface Cupom extends Codificavel {

    /** Se o pedido cumpre a condição da promoção. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    BigDecimal desconto(ContextoCupom contexto);
}
