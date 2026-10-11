package com.loja.checkout.resumo.cupom;

import com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;

/**
 * Uma promoção: sua condição e seu desconto. Cada cupom novo do marketing é
 * uma implementação desta interface registrada em {@link Cupons}.
 */
public interface Cupom {

    /** Pedido sem cupom: nenhuma condição, nenhum desconto. */
    Cupom NENHUM = base -> Dinheiro.ZERO;

    BigDecimal desconto(BaseCupom base);

    /** Se o pedido cumpre a condição da promoção. */
    default boolean aplicavel(BaseCupom base) {
        return true;
    }
}
