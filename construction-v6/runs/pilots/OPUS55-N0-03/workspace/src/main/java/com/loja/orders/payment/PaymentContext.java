package com.loja.orders.payment;

import java.math.BigDecimal;

/**
 * @param orderTotal o total do pedido (produtos − cupom + frete + seguro)
 * @param couponInformed se o cliente informou um cupom, mesmo que tenha dado desconto zero
 */
public record PaymentContext(BigDecimal orderTotal, int installments, boolean couponInformed) {
}
