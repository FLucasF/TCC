package com.loja.checkout.cupom;

import com.loja.checkout.Identificavel;
import com.loja.checkout.Pedido;
import java.math.BigDecimal;

public interface Cupom extends Identificavel {
    boolean aplicavel(Pedido pedido);

    /** @param frete frete que aparece no resumo */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
