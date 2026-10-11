package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/**
 * Uma promoção. Cada cupom novo do marketing é uma implementação desta interface:
 * diz quando pode ser usado e quanto desconta.
 */
public interface Cupom extends Identificavel {

    /** Se o pedido cumpre a condição do cupom (valor mínimo, itens exigidos, o que for). */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Desconto do cupom, arredondado para centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
