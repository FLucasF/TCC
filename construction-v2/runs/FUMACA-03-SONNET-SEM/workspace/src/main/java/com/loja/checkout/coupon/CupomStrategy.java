package com.loja.checkout.coupon;

import com.loja.checkout.PedidoContext;

import java.math.BigDecimal;

/**
 * Implemente esta interface e anote com {@code @Component} para cadastrar um novo cupom.
 */
public interface CupomStrategy {

    String getCodigo();

    boolean aplicavel(PedidoContext pedido, BigDecimal subtotalProdutos);

    BigDecimal calcularDesconto(PedidoContext pedido, BigDecimal subtotalProdutos, BigDecimal frete);
}
