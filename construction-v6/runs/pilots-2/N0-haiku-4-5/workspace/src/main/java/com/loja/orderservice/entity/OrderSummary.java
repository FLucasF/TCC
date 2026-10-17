package com.loja.orderservice.entity;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class OrderSummary {
    private BigDecimal productsSubtotal;
    private BigDecimal couponDiscount;
    private BigDecimal shippingCost;
    private Integer deliveryDays;
    private BigDecimal insurance;
    private BigDecimal paymentAdjustment;
    private BigDecimal finalTotal;
    private Integer installments;
    private BigDecimal installmentAmount;
    private BigDecimal nextPurchaseCredit;
    private Boolean gift;

    public OrderSummary() {}

    public OrderSummary(BigDecimal productsSubtotal, BigDecimal couponDiscount, BigDecimal shippingCost,
                        Integer deliveryDays, BigDecimal insurance, BigDecimal paymentAdjustment,
                        BigDecimal finalTotal, Integer installments, BigDecimal installmentAmount,
                        BigDecimal nextPurchaseCredit, Boolean gift) {
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

    public BigDecimal getProductsSubtotal() { return productsSubtotal; }
    public void setProductsSubtotal(BigDecimal productsSubtotal) { this.productsSubtotal = productsSubtotal; }

    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(BigDecimal couponDiscount) { this.couponDiscount = couponDiscount; }

    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }

    public Integer getDeliveryDays() { return deliveryDays; }
    public void setDeliveryDays(Integer deliveryDays) { this.deliveryDays = deliveryDays; }

    public BigDecimal getInsurance() { return insurance; }
    public void setInsurance(BigDecimal insurance) { this.insurance = insurance; }

    public BigDecimal getPaymentAdjustment() { return paymentAdjustment; }
    public void setPaymentAdjustment(BigDecimal paymentAdjustment) { this.paymentAdjustment = paymentAdjustment; }

    public BigDecimal getFinalTotal() { return finalTotal; }
    public void setFinalTotal(BigDecimal finalTotal) { this.finalTotal = finalTotal; }

    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }

    public BigDecimal getInstallmentAmount() { return installmentAmount; }
    public void setInstallmentAmount(BigDecimal installmentAmount) { this.installmentAmount = installmentAmount; }

    public BigDecimal getNextPurchaseCredit() { return nextPurchaseCredit; }
    public void setNextPurchaseCredit(BigDecimal nextPurchaseCredit) { this.nextPurchaseCredit = nextPurchaseCredit; }

    public Boolean getGift() { return gift; }
    public void setGift(Boolean gift) { this.gift = gift; }
}
