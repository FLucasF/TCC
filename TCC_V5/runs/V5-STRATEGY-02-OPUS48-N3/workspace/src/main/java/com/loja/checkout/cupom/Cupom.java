package com.loja.checkout.cupom;

import com.loja.checkout.comum.Codificavel;

import java.math.BigDecimal;

/**
 * Um cupom. Cada promoção tem sua condição de aplicabilidade e seu desconto.
 * Promoção nova = uma classe nova implementando isto.
 */
public interface Cupom extends Codificavel {

    /** Se o pedido cumpre a condição do cupom (ex.: MENOS50 a partir de R$ 300). */
    boolean aplicavel(CupomContexto ctx);

    /** Desconto já arredondado para centavos. */
    BigDecimal desconto(CupomContexto ctx);
}
