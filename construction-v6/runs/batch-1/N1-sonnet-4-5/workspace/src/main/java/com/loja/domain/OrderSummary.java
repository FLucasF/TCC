package com.loja.domain;

public record OrderSummary(
    Money productsSubtotal,
    Money couponDiscount,
    Money shippingCost,
    int deliveryDays,
    Money insurance,
    Money paymentAdjustment,
    Money finalTotal,
    int installments,
    Money installmentAmount,
    Money nextPurchaseCredit,
    boolean gift
) {}
