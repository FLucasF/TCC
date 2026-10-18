package com.example.api.controller;

import com.example.api.dto.*;
import com.example.application.service.OrderService;
import com.example.domain.entity.Order;
import com.example.domain.entity.OrderSummary;
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
    public ResponseEntity<OrderDto> createOrder(@RequestBody CheckoutRequestDto request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable String id) {
        Order order = orderService.getOrder(id);
        return ResponseEntity.ok(convertToDto(order));
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<OrderDto> recordPayment(@PathVariable String id, @RequestBody PaymentRequestDto paymentRequest) {
        Order order = orderService.processPayment(id, paymentRequest);
        return ResponseEntity.ok(convertToDto(order));
    }

    @PostMapping("/{id}/shipment")
    public ResponseEntity<OrderDto> recordShipment(@PathVariable String id, @RequestBody ShipmentRequestDto shipmentRequest) {
        Order order = orderService.shipOrder(id, shipmentRequest);
        return ResponseEntity.ok(convertToDto(order));
    }

    @PostMapping("/{id}/delivery")
    public ResponseEntity<OrderDto> recordDelivery(@PathVariable String id) {
        Order order = orderService.deliverOrder(id);
        return ResponseEntity.ok(convertToDto(order));
    }

    @PostMapping("/{id}/cancellation")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable String id) {
        Order order = orderService.cancelOrder(id);
        return ResponseEntity.ok(convertToDto(order));
    }

    private OrderDto convertToDto(Order order) {
        OrderSummary summary = order.getSummary();
        SummaryDto summaryDto = new SummaryDto(
                summary.getProductsSubtotal(),
                summary.getCouponDiscount(),
                summary.getShippingCost(),
                summary.getDeliveryDays(),
                summary.getInsurance(),
                summary.getPaymentAdjustment(),
                summary.getFinalTotal(),
                summary.getInstallments(),
                summary.getInstallmentAmount(),
                summary.getNextPurchaseCredit(),
                summary.getGift()
        );

        var itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(item.getSku(), item.getQuantity()))
                .toList();

        return new OrderDto(
                order.getId(),
                order.getStatus().name(),
                order.getCreatedAt(),
                summaryDto,
                itemDtos,
                order.getTrackingCode()
        );
    }
}
