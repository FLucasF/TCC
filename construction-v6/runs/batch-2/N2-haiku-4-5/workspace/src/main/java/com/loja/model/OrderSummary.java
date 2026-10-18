package com.loja.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderSummary {
    public Double productsSubtotal;
    public Double couponDiscount;
    public Double shippingCost;
    public Integer deliveryDays;
    public Double insurance;
    public Double paymentAdjustment;
    public Double finalTotal;
    public Integer installments;
    public Double installmentAmount;
    public Double nextPurchaseCredit;
    public Boolean gift;

    public OrderSummary() {}

    public OrderSummary(Double productsSubtotal, Double couponDiscount, Double shippingCost,
                       Integer deliveryDays, Double insurance, Double paymentAdjustment,
                       Double finalTotal, Integer installments, Double installmentAmount,
                       Double nextPurchaseCredit, Boolean gift) {
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
}
