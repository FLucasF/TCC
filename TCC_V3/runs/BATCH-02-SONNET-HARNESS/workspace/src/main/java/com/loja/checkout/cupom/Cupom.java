package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    boolean aplicavel(Pedido pedido);

    BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete);
}
