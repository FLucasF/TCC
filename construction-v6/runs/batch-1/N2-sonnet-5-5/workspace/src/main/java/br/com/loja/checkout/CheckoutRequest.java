package br.com.loja.checkout;

import java.util.List;

public record CheckoutRequest(Customer customer, List<Item> items, String zipCode, String region,
                              String shippingMethod, String coupon, String paymentMethod,
                              Integer installments, String clubTier) {

    public record Customer(String cpf, String email) {
    }

    public record Item(String sku, Integer quantity) {
    }
}
