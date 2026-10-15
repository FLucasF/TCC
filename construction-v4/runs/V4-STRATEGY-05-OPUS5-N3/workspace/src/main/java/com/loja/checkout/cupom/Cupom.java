package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Registro;
import java.math.BigDecimal;

/** Uma promocao do marketing. So vale um cupom por pedido. */
public interface Cupom extends Registro.Identificado {

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    BigDecimal desconto(ContextoCupom contexto);
}
