package com.loja.checkout.entrega;

import com.loja.checkout.service.PedidoContext;

import java.math.BigDecimal;

public interface OpcaoEntrega {

    String getCodigo();

    boolean disponivelPara(PedidoContext pedido);

    BigDecimal calcularFrete(PedidoContext pedido);

    int prazoDias();
}
