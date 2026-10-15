package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Identificavel;
import br.com.loja.checkout.Pedido;
import java.math.BigDecimal;

public interface Cupom extends Identificavel {

    default boolean aplicavel(Pedido pedido) {
        return true;
    }

    /** @param frete frete efetivamente cobrado do pedido */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
