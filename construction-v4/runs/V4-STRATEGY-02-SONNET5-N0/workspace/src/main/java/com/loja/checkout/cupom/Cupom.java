package com.loja.checkout.cupom;

import com.loja.checkout.service.PedidoContext;

import java.math.BigDecimal;

public interface Cupom {

    String getCodigo();

    boolean aplicavel(PedidoContext pedido);

    BigDecimal calcularDesconto(PedidoContext pedido, BigDecimal freteCalculado);
}
