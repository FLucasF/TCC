package com.loja.checkout.entrega;

import com.loja.checkout.pedido.Pedido;

import java.math.BigDecimal;

public interface MetodoEntrega {

    String codigo();

    boolean disponivel(Pedido pedido);

    BigDecimal custo(Pedido pedido);

    int prazoDias();
}
