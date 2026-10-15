package com.loja.checkout.cupom;

import com.loja.checkout.Pedido;
import java.math.BigDecimal;

public interface Cupom {
    String codigo();
    boolean aplicavel(Pedido pedido);
    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
