package br.com.loja.pedidos.checkout;

import java.math.BigDecimal;

public record Summary(
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
