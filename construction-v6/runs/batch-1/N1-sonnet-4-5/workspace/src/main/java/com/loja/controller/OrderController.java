package com.loja.controller;

import com.loja.domain.Order;
import com.loja.dto.*;
import com.loja.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(201).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        Order order = orderService.getOrder(id);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<OrderResponse> processPayment(
            @PathVariable Long id,
            @RequestBody PaymentRequest request) {
        Order order = orderService.processPayment(id, request);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/shipment")
    public ResponseEntity<OrderResponse> shipOrder(
            @PathVariable Long id,
            @RequestBody ShipmentRequest request) {
        Order order = orderService.shipOrder(id, request.trackingCode());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/delivery")
    public ResponseEntity<OrderResponse> deliverOrder(@PathVariable Long id) {
        Order order = orderService.deliverOrder(id);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/cancellation")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        Order order = orderService.cancelOrder(id);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
