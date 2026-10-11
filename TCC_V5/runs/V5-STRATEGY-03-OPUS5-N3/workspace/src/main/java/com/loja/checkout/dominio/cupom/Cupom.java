package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/**
 * Uma promoção. Cada cupom é dono da sua condição e da sua conta: uma promoção nova é uma
 * classe nova neste pacote, que só precisa dizer quanto abate — o arredondamento é igual
 * para todas.
 */
public interface Cupom extends Identificavel {

    /** Quanto o cupom abate, sem arredondar. */
    BigDecimal valorAbatido(ContextoCupom contexto);

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    default BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.arredonda(valorAbatido(contexto));
    }
}
