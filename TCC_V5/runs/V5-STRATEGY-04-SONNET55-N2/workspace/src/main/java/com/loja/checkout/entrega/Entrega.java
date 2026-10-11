package com.loja.checkout.entrega;

import com.loja.checkout.Identificavel;
import com.loja.checkout.Pedido;
import java.math.BigDecimal;

public interface Entrega extends Identificavel {
    boolean atende(Pedido pedido);

    BigDecimal frete(Pedido pedido);

    int prazoDias();
}
