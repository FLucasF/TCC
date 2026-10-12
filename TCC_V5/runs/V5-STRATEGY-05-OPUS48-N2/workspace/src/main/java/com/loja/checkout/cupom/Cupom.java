package com.loja.checkout.cupom;

import com.loja.checkout.comum.Codificavel;

import java.math.BigDecimal;

/**
 * Um cupom de desconto. O marketing inventa promoção o tempo todo, cada uma
 * com sua regra, então cada cupom mora numa classe própria e é escolhido pelo
 * código, não por uma cadeia de condições.
 */
public interface Cupom extends Codificavel {

    /** O pedido cumpre a condição do cupom? (ex.: MENOS50 a partir de R$ 300) */
    boolean aplicavel(ContextoCupom ctx);

    /** Valor do desconto, já em centavos. Só chamado quando aplicavel. */
    BigDecimal desconto(ContextoCupom ctx);
}
