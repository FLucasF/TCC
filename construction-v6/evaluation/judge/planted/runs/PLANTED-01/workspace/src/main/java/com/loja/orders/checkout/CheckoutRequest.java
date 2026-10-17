package com.loja.orders.checkout;

import java.util.List;

import com.loja.orders.order.OrderItem;

/**
 * Os dados da compra. O carrinho e o cliente já vêm conferidos; os demais campos vêm como o site
 * mandou ({@code null} quando não vieram) e são conferidos no cálculo, na ordem combinada.
 *
 * @param couponInformed se o campo {@code coupon} veio
 * @param coupon o código do cupom, ou {@code null} se não veio ou não é texto
 * @param installments as parcelas (1 quando não vieram), ou {@code null} se não é um número inteiro
 */
public record CheckoutRequest(
        String cpf,
        String email,
        List<OrderItem> items,
        String zipCode,
        String region,
        String shippingMethod,
        boolean couponInformed,
        String coupon,
        String paymentMethod,
        Integer installments,
        String clubTier) {
}
