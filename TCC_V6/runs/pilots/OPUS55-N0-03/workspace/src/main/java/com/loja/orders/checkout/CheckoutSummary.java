package com.loja.orders.checkout;

import java.math.BigDecimal;

/** O resumo da compra: o mesmo cálculo serve para o resumo e para o pedido registrado. */
public record CheckoutSummary(
        BigDecimal productsSubtotal,
        BigDecimal couponDiscount,
        BigDecimal shippingCost,
        int deliveryDays,
        BigDecimal insurance,
        BigDecimal paymentAdjustment,
        BigDecimal finalTotal,
        int installments,
        BigDecimal installmentAmount,
        BigDecimal nextPurchaseCredit,
        boolean gift) {
}
