package com.loja.checkout.cupom;

import com.loja.checkout.domain.PedidoContexto;

import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    boolean aplicavel(PedidoContexto pedido, BigDecimal frete);

    BigDecimal calcularDesconto(PedidoContexto pedido, BigDecimal frete);
}
