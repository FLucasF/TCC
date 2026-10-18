package com.loja.models;

public class OrderSummary {
    public double productsSubtotal;
    public double couponDiscount;
    public double shippingCost;
    public int deliveryDays;
    public double insurance;
    public double paymentAdjustment;
    public double finalTotal;
    public int installments;
    public double installmentAmount;
    public double nextPurchaseCredit;
    public boolean gift;

    public OrderSummary() {}

    public OrderSummary(double productsSubtotal, double couponDiscount, double shippingCost,
                       int deliveryDays, double insurance, double paymentAdjustment,
                       double finalTotal, int installments, double installmentAmount,
                       double nextPurchaseCredit, boolean gift) {
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
