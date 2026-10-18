package com.loja.dto;

import com.loja.domain.OrderSummary;
import java.math.BigDecimal;

public record OrderSummaryResponse(
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
    boolean gift
) {
    public static OrderSummaryResponse from(OrderSummary summary) {
        return new OrderSummaryResponse(
            summary.productsSubtotal().value(),
            summary.couponDiscount().value(),
            summary.shippingCost().value(),
            summary.deliveryDays(),
            summary.insurance().value(),
            summary.paymentAdjustment().value(),
            summary.finalTotal().value(),
            summary.installments(),
            summary.installmentAmount().value(),
            summary.nextPurchaseCredit().value(),
            summary.gift()
        );
    }
}
