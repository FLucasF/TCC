package shop.checkout;

import shop.Money;

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

    public Summary {
        productsSubtotal = Money.round(productsSubtotal);
        couponDiscount = Money.round(couponDiscount);
        shippingCost = Money.round(shippingCost);
        insurance = Money.round(insurance);
        paymentAdjustment = Money.round(paymentAdjustment);
        finalTotal = Money.round(finalTotal);
        installmentAmount = Money.round(installmentAmount);
        nextPurchaseCredit = Money.round(nextPurchaseCredit);
    }
}
