package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

/**
 * Um cupom de desconto. Cada promoção tem sua regra própria de condição e de
 * valor, então cada cupom mora numa implementação própria. Entra cupom novo
 * adicionando uma nova implementação.
 */
public interface Cupom {

    String codigo();

    /** Se o pedido cumpre a condição do cupom. Por padrão, cumpre. */
    default boolean aplicavel(CupomContexto ctx) {
        return true;
    }

    /** Valor do desconto (antes do arredondamento de centavos). */
    BigDecimal desconto(CupomContexto ctx);
}
