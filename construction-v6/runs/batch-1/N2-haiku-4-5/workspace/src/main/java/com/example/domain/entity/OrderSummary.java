package com.example.domain.entity;

import java.math.BigDecimal;

public class OrderSummary {
    private final BigDecimal productsSubtotal;
    private final BigDecimal couponDiscount;
    private final BigDecimal shippingCost;
    private final Integer deliveryDays;
    private final BigDecimal insurance;
    private final BigDecimal paymentAdjustment;
    private final BigDecimal finalTotal;
    private final Integer installments;
    private final BigDecimal installmentAmount;
    private final BigDecimal nextPurchaseCredit;
    private final Boolean gift;

    public OrderSummary(
            BigDecimal productsSubtotal,
            BigDecimal couponDiscount,
            BigDecimal shippingCost,
            Integer deliveryDays,
            BigDecimal insurance,
            BigDecimal paymentAdjustment,
            BigDecimal finalTotal,
            Integer installments,
            BigDecimal installmentAmount,
            BigDecimal nextPurchaseCredit,
            Boolean gift) {
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

    public BigDecimal getProductsSubtotal() {
        return productsSubtotal;
    }

    public BigDecimal getCouponDiscount() {
        return couponDiscount;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public Integer getDeliveryDays() {
        return deliveryDays;
    }

    public BigDecimal getInsurance() {
        return insurance;
    }

    public BigDecimal getPaymentAdjustment() {
        return paymentAdjustment;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }

    public Integer getInstallments() {
        return installments;
    }

    public BigDecimal getInstallmentAmount() {
        return installmentAmount;
    }

    public BigDecimal getNextPurchaseCredit() {
        return nextPurchaseCredit;
    }

    public Boolean getGift() {
        return gift;
    }
}
