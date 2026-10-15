package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Codificado;

import java.math.BigDecimal;

/** Uma promocao do marketing: sua condicao e sua forma de descontar. */
public interface Cupom extends Codificado {

    BigDecimal desconto(ContextoCupom contexto);

    /** Se o pedido cumpre a condicao do cupom. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
