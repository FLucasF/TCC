package com.loja.model;

import java.util.List;

public class CheckoutRequest {
    public CustomerRequest customer;
    public List<OrderItem> items;
    public String zipCode;
    public Region region;
    public ShippingMethod shippingMethod;
    public String coupon;
    public PaymentMethod paymentMethod;
    public Integer installments;
    public ClubTier clubTier;

    public CheckoutRequest() {
        this.installments = 1;
    }
}
