package com.loja.orderservice.controller;

import com.loja.orderservice.dto.*;
import com.loja.orderservice.exception.OrderServiceException;
import com.loja.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CheckoutSummaryRequest request) {
        try {
            OrderResponse response = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (OrderServiceException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(ErrorResponse.builder().error(e.getErrorCode()).build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        try {
            OrderResponse response = orderService.getOrder(id);
            return ResponseEntity.ok(response);
        } catch (OrderServiceException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(ErrorResponse.builder().error(e.getErrorCode()).build());
        }
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<?> processPayment(@PathVariable String id,
                                           @RequestBody PaymentNotificationRequest request) {
        try {
            OrderResponse response = orderService.processPayment(id, request);
            return ResponseEntity.ok(response);
        } catch (OrderServiceException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(ErrorResponse.builder().error(e.getErrorCode()).build());
        }
    }

    @PostMapping("/{id}/shipment")
    public ResponseEntity<?> shipOrder(@PathVariable String id,
                                      @RequestBody ShipmentRequest request) {
        try {
            OrderResponse response = orderService.shipOrder(id, request.getTrackingCode());
            return ResponseEntity.ok(response);
        } catch (OrderServiceException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(ErrorResponse.builder().error(e.getErrorCode()).build());
        }
    }

    @PostMapping("/{id}/delivery")
    public ResponseEntity<?> deliverOrder(@PathVariable String id) {
        try {
            OrderResponse response = orderService.deliverOrder(id);
            return ResponseEntity.ok(response);
        } catch (OrderServiceException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(ErrorResponse.builder().error(e.getErrorCode()).build());
        }
    }

    @PostMapping("/{id}/cancellation")
    public ResponseEntity<?> cancelOrder(@PathVariable String id) {
        try {
            OrderResponse response = orderService.cancelOrder(id);
            return ResponseEntity.ok(response);
        } catch (OrderServiceException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(ErrorResponse.builder().error(e.getErrorCode()).build());
        }
    }
}
