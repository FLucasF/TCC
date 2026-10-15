package com.loja.checkout.cupom;

import com.loja.checkout.comum.Identificavel;
import java.math.BigDecimal;

/**
 * Uma promocao do marketing. Cada cupom novo e um bean novo implementando esta interface.
 */
public interface Cupom extends Identificavel {

    /** Desconto do cupom, arredondado para centavos. */
    BigDecimal calcularDesconto(ContextoCupom contexto);

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
