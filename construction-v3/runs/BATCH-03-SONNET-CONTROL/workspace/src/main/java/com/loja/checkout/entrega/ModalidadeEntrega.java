package com.loja.checkout.entrega;

import com.loja.checkout.domain.PedidoContexto;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    boolean disponivelPara(PedidoContexto pedido);

    BigDecimal calcularFrete(PedidoContexto pedido);

    int prazoDias();
}
