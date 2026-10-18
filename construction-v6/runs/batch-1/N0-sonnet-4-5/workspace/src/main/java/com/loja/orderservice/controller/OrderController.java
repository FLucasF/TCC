package com.loja.orderservice.controller;

import com.loja.orderservice.dto.*;
import com.loja.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CheckoutRequest request) {
        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        OrderResponse order = orderService.getOrder(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<OrderResponse> processPayment(
            @PathVariable Long id,
            @RequestBody PaymentRequest request) {
        OrderResponse order = orderService.processPayment(id, request);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/shipment")
    public ResponseEntity<OrderResponse> shipOrder(
            @PathVariable Long id,
            @RequestBody ShipmentRequest request) {
        OrderResponse order = orderService.shipOrder(id, request);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/delivery")
    public ResponseEntity<OrderResponse> deliverOrder(@PathVariable Long id) {
        OrderResponse order = orderService.deliverOrder(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/cancellation")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        OrderResponse order = orderService.cancelOrder(id);
        return ResponseEntity.ok(order);
    }
}
