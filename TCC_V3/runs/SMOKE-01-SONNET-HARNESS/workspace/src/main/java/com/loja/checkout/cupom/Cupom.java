package com.loja.checkout.cupom;

import com.loja.checkout.domain.PedidoContext;

import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    boolean aplicavel(PedidoContext contexto);

    BigDecimal calcularDesconto(PedidoContext contexto);
}
