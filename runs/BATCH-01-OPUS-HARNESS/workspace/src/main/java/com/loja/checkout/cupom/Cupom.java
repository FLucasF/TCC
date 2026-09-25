package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Codificavel;

import java.math.BigDecimal;

/**
 * Uma promocao. Cada uma decide sozinha se vale para o pedido e quanto desconta.
 */
public interface Cupom extends Codificavel {

    BigDecimal desconto(ContextoCupom contexto);

    /** Por padrao a promocao vale para qualquer pedido. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
