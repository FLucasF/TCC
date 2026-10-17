package com.loja.orders.order;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.loja.orders.catalog.ProductController;

@Service
public class OrderReceipts {

    private final OrderService orders;
    private final ProductController products;

    public OrderReceipts(OrderService orders, ProductController products) {
        this.orders = orders;
        this.products = products;
    }

    public ResponseEntity<OrderResponse> receipt(String orderId) {
        return ResponseEntity.ok(OrderResponse.of(orders.get(orderId)));
    }
}
