package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Identificavel;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma promocao que o marketing criou. Promocao nova = um bean novo
 * implementando esta interface.
 */
public interface Cupom extends Identificavel {

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }

    /** Quanto o cupom desconta deste pedido. */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
