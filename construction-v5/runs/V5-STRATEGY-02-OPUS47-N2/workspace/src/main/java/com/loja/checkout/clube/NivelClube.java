package com.loja.checkout.clube;

import com.loja.checkout.Pedido;
import java.math.BigDecimal;

public interface NivelClube {
    String codigo();
    BigDecimal ajustarFrete(BigDecimal frete, Pedido pedido);
    BigDecimal credito(Pedido pedido);
    boolean brinde(Pedido pedido);
}
