package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Codificavel;

import java.math.BigDecimal;

/**
 * Uma promocao. Cada cupom tem sua condicao e sua forma de descontar; um cupom
 * novo e uma classe nova, e nada mais.
 */
public interface Cupom extends Codificavel {

    /** Se o pedido cumpre a condicao do cupom. */
    boolean aplicavel(BaseCupom base);

    BigDecimal desconto(BaseCupom base);
}
