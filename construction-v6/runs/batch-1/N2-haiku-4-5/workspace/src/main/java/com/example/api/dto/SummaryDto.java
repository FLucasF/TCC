package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class SummaryDto {
    @JsonProperty
    private BigDecimal productsSubtotal;

    @JsonProperty
    private BigDecimal couponDiscount;

    @JsonProperty
    private BigDecimal shippingCost;

    @JsonProperty
    private Integer deliveryDays;

    @JsonProperty
    private BigDecimal insurance;

    @JsonProperty
    private BigDecimal paymentAdjustment;

    @JsonProperty
    private BigDecimal finalTotal;

    @JsonProperty
    private Integer installments;

    @JsonProperty
    private BigDecimal installmentAmount;

    @JsonProperty
    private BigDecimal nextPurchaseCredit;

    @JsonProperty
    private Boolean gift;

    public SummaryDto(
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
