package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

public interface OpcaoEntrega {

    String codigo();

    boolean disponivelPara(Pedido pedido);

    BigDecimal calcularFrete(Pedido pedido);

    int prazoDias();
}
