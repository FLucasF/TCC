package com.loja.orders.order;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.loja.orders.checkout.CheckoutSummary;

public record OrderResponse(
        String id,
        String status,
        String createdAt,
        CheckoutSummary summary,
        List<OrderItem> items,
        String trackingCode) {

    public static OrderResponse of(Order order) {
        return new OrderResponse(
                order.id(),
                order.status().name(),
                DateTimeFormatter.ISO_INSTANT.format(order.createdAt()),
                order.summary(),
                order.items(),
                order.trackingCode());
    }
}
