package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CheckoutRequestDto {
    @JsonProperty
    private CustomerRequestDto customer;

    @JsonProperty
    private List<OrderItemDto> items;

    @JsonProperty
    private String zipCode;

    @JsonProperty
    private String region;

    @JsonProperty
    private String shippingMethod;

    @JsonProperty
    private String coupon;

    @JsonProperty
    private String paymentMethod;

    @JsonProperty
    private Integer installments;

    @JsonProperty
    private String clubTier;

    public CustomerRequestDto getCustomer() {
        return customer;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getRegion() {
        return region;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public String getCoupon() {
        return coupon;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public Integer getInstallments() {
        return installments;
    }

    public String getClubTier() {
        return clubTier;
    }

    public void setCustomer(CustomerRequestDto customer) {
        this.customer = customer;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public void setCoupon(String coupon) {
        this.coupon = coupon;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setInstallments(Integer installments) {
        this.installments = installments;
    }

    public void setClubTier(String clubTier) {
        this.clubTier = clubTier;
    }
}
