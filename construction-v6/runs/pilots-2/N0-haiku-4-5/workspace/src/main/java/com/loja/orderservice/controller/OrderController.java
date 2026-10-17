package com.loja.orderservice.controller;

import com.loja.orderservice.dto.*;
import com.loja.orderservice.entity.Order;
import com.loja.orderservice.entity.OrderStatus;
import com.loja.orderservice.entity.OrderSummary;
import com.loja.orderservice.exception.OrderException;
import com.loja.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout/summary")
    public ResponseEntity<?> calculateSummary(@RequestBody OrderRequestDTO request) {
        try {
            OrderSummary summary = orderService.calculateSummary(request);
            SummaryResponseDTO response = convertToSummaryDTO(summary);
            return ResponseEntity.ok(response);
        } catch (OrderException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(new ErrorResponseDTO(e.getErrorCode()));
        }
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequestDTO request) {
        try {
            Order order = orderService.createOrder(request);
            OrderResponseDTO response = convertToOrderDTO(order);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (OrderException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(new ErrorResponseDTO(e.getErrorCode()));
        }
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        try {
            Order order = orderService.getOrder(id);
            OrderResponseDTO response = convertToOrderDTO(order);
            return ResponseEntity.ok(response);
        } catch (OrderException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(new ErrorResponseDTO(e.getErrorCode()));
        }
    }

    @PostMapping("/orders/{id}/payment")
    public ResponseEntity<?> processPayment(@PathVariable String id, @RequestBody PaymentRequestDTO request) {
        try {
            if (request.getTransactionId() == null || request.getPaidAt() == null) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponseDTO("INVALID_ORDER"));
            }

            Order order = orderService.processPayment(id, request.getTransactionId(), request.getPaidAt());
            OrderResponseDTO response = convertToOrderDTO(order);
            return ResponseEntity.ok(response);
        } catch (OrderException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(new ErrorResponseDTO(e.getErrorCode()));
        }
    }

    @PostMapping("/orders/{id}/shipment")
    public ResponseEntity<?> shipOrder(@PathVariable String id, @RequestBody ShipmentRequestDTO request) {
        try {
            if (request.getTrackingCode() == null) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponseDTO("INVALID_ORDER"));
            }

            Order order = orderService.shipOrder(id, request.getTrackingCode());
            OrderResponseDTO response = convertToOrderDTO(order);
            return ResponseEntity.ok(response);
        } catch (OrderException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(new ErrorResponseDTO(e.getErrorCode()));
        }
    }

    @PostMapping("/orders/{id}/delivery")
    public ResponseEntity<?> deliverOrder(@PathVariable String id) {
        try {
            Order order = orderService.deliverOrder(id);
            OrderResponseDTO response = convertToOrderDTO(order);
            return ResponseEntity.ok(response);
        } catch (OrderException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(new ErrorResponseDTO(e.getErrorCode()));
        }
    }

    @PostMapping("/orders/{id}/cancellation")
    public ResponseEntity<?> cancelOrder(@PathVariable String id) {
        try {
            Order order = orderService.cancelOrder(id);
            OrderResponseDTO response = convertToOrderDTO(order);
            return ResponseEntity.ok(response);
        } catch (OrderException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(new ErrorResponseDTO(e.getErrorCode()));
        }
    }

    private OrderResponseDTO convertToOrderDTO(Order order) {
        java.util.List<OrderItemDTO> items = order.getItems().stream()
            .map(item -> new OrderItemDTO(item.getSku(), item.getQuantity()))
            .collect(Collectors.toList());

        return new OrderResponseDTO(
            order.getId(),
            order.getStatus(),
            order.getCreatedAt(),
            order.getSummary(),
            items,
            order.getTrackingCode()
        );
    }

    private SummaryResponseDTO convertToSummaryDTO(OrderSummary summary) {
        return new SummaryResponseDTO(
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
    }
}
