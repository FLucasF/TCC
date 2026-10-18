package com.loja.api.dto;

import java.util.List;

public class CheckoutRequestDto {
    public CustomerDto customer;
    public List<OrderItemDto> items;
    public String zipCode;
    public String region;
    public String shippingMethod;
    public String coupon;
    public String paymentMethod;
    public Integer installments;
    public String clubTier;

    public static class CustomerDto {
        public String cpf;
        public String email;
    }

    public static class OrderItemDto {
        public String sku;
        public int quantity;
    }
}
