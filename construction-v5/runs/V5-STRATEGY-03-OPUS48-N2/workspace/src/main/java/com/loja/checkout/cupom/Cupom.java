package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Cupom de desconto. O marketing inventa promocao o tempo todo, cada uma com
 * sua regra de desconto e sua condicao. Cada cupom mora numa classe propria;
 * um cupom novo e uma nova implementacao.
 */
public interface Cupom {

    String codigo();

    /** Se o pedido cumpre a condicao do cupom. */
    default boolean aplicavel(CupomContexto contexto) {
        return true;
    }

    /** Valor do desconto sobre o pedido. */
    BigDecimal desconto(CupomContexto contexto);
}
