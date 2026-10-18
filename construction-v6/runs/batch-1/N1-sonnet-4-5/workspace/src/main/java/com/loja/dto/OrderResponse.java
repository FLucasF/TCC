package com.loja.dto;

import com.loja.domain.Order;
import com.loja.domain.OrderItem;
import com.loja.domain.OrderStatus;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    OrderStatus status,
    Instant createdAt,
    OrderSummaryResponse summary,
    List<ItemInfo> items,
    String trackingCode
) {
    public record ItemInfo(String sku, int quantity) {}

    public static OrderResponse from(Order order) {
        List<ItemInfo> items = order.getItems().stream()
            .map(item -> new ItemInfo(item.getSku(), item.getQuantity()))
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getStatus(),
            order.getCreatedAt(),
            OrderSummaryResponse.from(order.getSummary()),
            items,
            order.getTrackingCode()
        );
    }
}
