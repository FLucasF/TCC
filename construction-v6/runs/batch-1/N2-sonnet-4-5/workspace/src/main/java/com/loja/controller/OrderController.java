package com.loja.controller;

import com.loja.dto.*;
import com.loja.service.OrderService;
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
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<OrderResponse> processPayment(@PathVariable String id,
                                                        @RequestBody PaymentRequest request) {
        OrderResponse response = orderService.processPayment(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/shipment")
    public ResponseEntity<OrderResponse> shipOrder(@PathVariable String id,
                                                    @RequestBody ShipmentRequest request) {
        OrderResponse response = orderService.shipOrder(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/delivery")
    public ResponseEntity<OrderResponse> deliverOrder(@PathVariable String id) {
        OrderResponse response = orderService.deliverOrder(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancellation")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }
}
