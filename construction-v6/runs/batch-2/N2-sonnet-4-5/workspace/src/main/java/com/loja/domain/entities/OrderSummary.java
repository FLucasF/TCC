package com.loja.domain.entities;

public class OrderSummary {
    private final Money productsSubtotal;
    private final Money couponDiscount;
    private final Money shippingCost;
    private final int deliveryDays;
    private final Money insurance;
    private final Money paymentAdjustment;
    private final Money finalTotal;
    private final int installments;
    private final Money installmentAmount;
    private final Money nextPurchaseCredit;
    private final boolean gift;

    public OrderSummary(
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
            boolean gift) {
        this.productsSubtotal = productsSubtotal;
        this.couponDiscount = couponDiscount;
        this.shippingCost = shippingCost;
        this.deliveryDays = deliveryDays;
        this.insurance = insurance;
        this.paymentAdjustment = paymentAdjustment;
        this.finalTotal = finalTotal;
        this.installments = installments;
        this.installmentAmount = installmentAmount;
        this.nextPurchaseCredit = nextPurchaseCredit;
        this.gift = gift;
    }

    public Money getProductsSubtotal() {
        return productsSubtotal;
    }

    public Money getCouponDiscount() {
        return couponDiscount;
    }

    public Money getShippingCost() {
        return shippingCost;
    }

    public int getDeliveryDays() {
        return deliveryDays;
    }

    public Money getInsurance() {
        return insurance;
    }

    public Money getPaymentAdjustment() {
        return paymentAdjustment;
    }

    public Money getFinalTotal() {
        return finalTotal;
    }

    public int getInstallments() {
        return installments;
    }

    public Money getInstallmentAmount() {
        return installmentAmount;
    }

    public Money getNextPurchaseCredit() {
        return nextPurchaseCredit;
    }

    public boolean isGift() {
        return gift;
    }
}
