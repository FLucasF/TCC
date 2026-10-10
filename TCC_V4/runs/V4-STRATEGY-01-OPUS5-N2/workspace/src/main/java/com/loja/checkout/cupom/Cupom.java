package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Identificavel;
import java.math.BigDecimal;

/**
 * Uma promocao. Cada cupom novo do marketing e' uma implementacao desta
 * interface: traz sua conta de desconto e, se tiver, sua condicao.
 */
public interface Cupom extends Identificavel {

    /** Quanto este cupom desconta neste pedido. */
    BigDecimal desconto(ContextoCupom contexto);

    /** Se o pedido cumpre a condicao do cupom. Por padrao, nao tem condicao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
