package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

/**
 * Uma promoção. Cada cupom decide se cabe no pedido e quanto desconta. Um
 * cupom novo que o marketing inventar entra como uma nova implementação.
 */
public interface Cupom {

    /** Código exatamente como o site envia (ex.: "BEMVINDO10"). */
    String codigo();

    /** Se o cupom pode ser aplicado a este pedido. */
    boolean aplicavel(CupomContexto ctx);

    /** Desconto do cupom, já em centavos. Só chamado quando aplicável. */
    BigDecimal desconto(CupomContexto ctx);
}
