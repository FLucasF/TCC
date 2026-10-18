package com.loja.order.model;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class OrderSummary {
  private BigDecimal productsSubtotal;
  private BigDecimal couponDiscount;
  private BigDecimal shippingCost;
  private int deliveryDays;
  private BigDecimal insurance;
  private BigDecimal paymentAdjustment;
  private BigDecimal finalTotal;
  private int installments;
  private BigDecimal installmentAmount;
  private BigDecimal nextPurchaseCredit;
  private boolean gift;

  protected OrderSummary() {}

  public OrderSummary(
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

  public int getDeliveryDays() {
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

  public int getInstallments() {
    return installments;
  }

  public BigDecimal getInstallmentAmount() {
    return installmentAmount;
  }

  public BigDecimal getNextPurchaseCredit() {
    return nextPurchaseCredit;
  }

  public boolean isGift() {
    return gift;
  }
}
