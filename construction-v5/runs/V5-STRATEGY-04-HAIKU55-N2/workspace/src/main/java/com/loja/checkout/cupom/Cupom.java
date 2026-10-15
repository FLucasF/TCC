package com.loja.checkout.cupom;

import com.loja.checkout.calculo.ContextoCompra;
import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    BigDecimal desconto(ContextoCompra compra);

    default boolean aplicavel(ContextoCompra compra) {
        return true;
    }
}
