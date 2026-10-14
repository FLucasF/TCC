package com.loja.orders.order;

import java.time.Instant;
import java.util.List;

import com.loja.orders.checkout.CheckoutSummary;

public record Order(
        String id,
        String cpf,
        String email,
        OrderStatus status,
        Instant createdAt,
        CheckoutSummary summary,
        List<OrderItem> items,
        String trackingCode) {
}
