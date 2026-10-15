package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Codificado;

import java.math.BigDecimal;

/**
 * Um cupom de desconto. Cada cupom tem sua condição de uso e sua fórmula de
 * desconto, então cada um mora na sua própria classe. Promoções novas entram só
 * adicionando mais uma implementação.
 */
public interface Cupom extends Codificado {

    /** Se o pedido cumpre a condição do cupom (ex.: MENOS50 a partir de R$ 300). */
    default boolean aplicavel(ContextoCupom ctx) {
        return true;
    }

    /** O desconto que o cupom dá a este pedido. */
    BigDecimal desconto(ContextoCupom ctx);
}
