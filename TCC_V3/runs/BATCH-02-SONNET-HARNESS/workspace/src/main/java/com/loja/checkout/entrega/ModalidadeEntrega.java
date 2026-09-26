package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    boolean disponivelPara(Pedido pedido);

    BigDecimal calcularFrete(Pedido pedido);

    int prazoDias();
}
