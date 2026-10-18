package com.loja.api;

import java.util.List;

public class CheckoutRequest {
    public CustomerData customer;
    public List<OrderItemData> items;
    public String zipCode;
    public String region;
    public String shippingMethod;
    public String coupon;
    public String paymentMethod;
    public Integer installments;
    public String clubTier;

    public static class CustomerData {
        public String cpf;
        public String email;
    }

    public static class OrderItemData {
        public String sku;
        public int quantity;
    }
}
