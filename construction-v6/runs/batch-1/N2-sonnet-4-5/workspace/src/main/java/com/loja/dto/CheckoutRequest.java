package com.loja.dto;

import java.util.List;

public class CheckoutRequest {
    private CustomerInfo customer;
    private List<ItemRequest> items;
    private String zipCode;
    private String region;
    private String shippingMethod;
    private String coupon;
    private String paymentMethod;
    private Integer installments;
    private String clubTier;

    public CustomerInfo getCustomer() { return customer; }
    public void setCustomer(CustomerInfo customer) { this.customer = customer; }

    public List<ItemRequest> getItems() { return items; }
    public void setItems(List<ItemRequest> items) { this.items = items; }

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
}
