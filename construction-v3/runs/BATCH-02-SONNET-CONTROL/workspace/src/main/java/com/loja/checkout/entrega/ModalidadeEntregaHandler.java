package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntregaHandler {

    ModalidadeEntrega getModalidade();

    boolean disponivel(BigDecimal pesoPedidoKg);

    BigDecimal calcularFrete(BigDecimal pesoPedidoKg);

    int prazoEntregaDias();
}
