package com.loja.domain;

import com.loja.state.OrderState;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String id;

    @Column(name = "customer_cpf")
    private String customerCpf;

    @Column(name = "customer_email")
    private String customerEmail;

    private String status;

    @Column(name = "zip_code")
    private String zipCode;

    private String region;

    @Column(name = "shipping_method")
    private String shippingMethod;

    private String coupon;

    @Column(name = "payment_method")
    private String paymentMethod;

    private Integer installments;

    @Column(name = "club_tier")
    private String clubTier;

    @Column(name = "products_subtotal")
    private BigDecimal productsSubtotal;

    @Column(name = "coupon_discount")
    private BigDecimal couponDiscount;

    @Column(name = "shipping_cost")
    private BigDecimal shippingCost;

    @Column(name = "delivery_days")
    private Integer deliveryDays;

    private BigDecimal insurance;

    @Column(name = "payment_adjustment")
    private BigDecimal paymentAdjustment;

    @Column(name = "final_total")
    private BigDecimal finalTotal;

    @Column(name = "installment_amount")
    private BigDecimal installmentAmount;

    @Column(name = "next_purchase_credit")
    private BigDecimal nextPurchaseCredit;

    private Boolean gift;

    @Column(name = "tracking_code")
    private String trackingCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Transient
    private OrderState state;

    public Order() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerCpf() { return customerCpf; }
    public void setCustomerCpf(String customerCpf) { this.customerCpf = customerCpf; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public String getCoupon() { return coupon; }
    public void setCoupon(String coupon) { this.coupon = coupon; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }

    public String getClubTier() { return clubTier; }
    public void setClubTier(String clubTier) { this.clubTier = clubTier; }

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

    public BigDecimal getInstallmentAmount() { return installmentAmount; }
    public void setInstallmentAmount(BigDecimal installmentAmount) { this.installmentAmount = installmentAmount; }

    public BigDecimal getNextPurchaseCredit() { return nextPurchaseCredit; }
    public void setNextPurchaseCredit(BigDecimal nextPurchaseCredit) { this.nextPurchaseCredit = nextPurchaseCredit; }

    public Boolean getGift() { return gift; }
    public void setGift(Boolean gift) { this.gift = gift; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public OrderState getState() { return state; }
    public void setState(OrderState state) { this.state = state; }
}
