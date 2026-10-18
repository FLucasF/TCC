package com.loja.order.controller;

import com.loja.order.dto.*;
import com.loja.order.service.OrderService;
import com.loja.order.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/")
public class OrderController {
  private final OrderService orderService;
  private final NotificationService notificationService;

  public OrderController(OrderService orderService, NotificationService notificationService) {
    this.orderService = orderService;
    this.notificationService = notificationService;
  }

  @PostMapping("/checkout/summary")
  public ResponseEntity<?> calculateSummary(@RequestBody OrderRequest request) {
    try {
      SummaryResponse summary = orderService.calculateSummary(request);
      return ResponseEntity.ok(summary);
    } catch (OrderService.ValidationException e) {
      return ResponseEntity.status(422).body(new ErrorResponse(e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.status(503).body(new ErrorResponse("CARRIER_UNAVAILABLE"));
    }
  }

  @PostMapping("/orders")
  public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
    try {
      OrderResponse order = orderService.createOrder(request);
      return ResponseEntity.status(201).body(order);
    } catch (OrderService.ValidationException e) {
      return ResponseEntity.status(422).body(new ErrorResponse(e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.status(503).body(new ErrorResponse("CARRIER_UNAVAILABLE"));
    }
  }

  @GetMapping("/orders/{id}")
  public ResponseEntity<?> getOrder(@PathVariable Long id) {
    try {
      OrderResponse order = orderService.getOrder(id);
      return ResponseEntity.ok(order);
    } catch (OrderService.NotFoundException e) {
      return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
    }
  }

  @PostMapping("/orders/{id}/payment")
  public ResponseEntity<?> processPayment(@PathVariable Long id, @RequestBody PaymentRequest request) {
    try {
      OrderResponse order = orderService.processPayment(id, request);
      return ResponseEntity.ok(order);
    } catch (OrderService.ValidationException e) {
      return ResponseEntity.status(422).body(new ErrorResponse(e.getMessage()));
    } catch (OrderService.NotFoundException e) {
      return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
    } catch (OrderService.OperationNotAllowedException e) {
      return ResponseEntity.status(409).body(new ErrorResponse(e.getMessage()));
    }
  }

  @PostMapping("/orders/{id}/shipment")
  public ResponseEntity<?> shipOrder(@PathVariable Long id, @RequestBody ShipmentRequest request) {
    try {
      OrderResponse order = orderService.shipOrder(id, request);
      return ResponseEntity.ok(order);
    } catch (OrderService.ValidationException e) {
      return ResponseEntity.status(422).body(new ErrorResponse(e.getMessage()));
    } catch (OrderService.NotFoundException e) {
      return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
    } catch (OrderService.OperationNotAllowedException e) {
      return ResponseEntity.status(409).body(new ErrorResponse(e.getMessage()));
    }
  }

  @PostMapping("/orders/{id}/delivery")
  public ResponseEntity<?> deliverOrder(@PathVariable Long id) {
    try {
      OrderResponse order = orderService.deliverOrder(id);
      return ResponseEntity.ok(order);
    } catch (OrderService.NotFoundException e) {
      return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
    } catch (OrderService.OperationNotAllowedException e) {
      return ResponseEntity.status(409).body(new ErrorResponse(e.getMessage()));
    }
  }

  @PostMapping("/orders/{id}/cancellation")
  public ResponseEntity<?> cancelOrder(@PathVariable Long id) {
    try {
      OrderResponse order = orderService.cancelOrder(id);
      return ResponseEntity.ok(order);
    } catch (OrderService.NotFoundException e) {
      return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
    } catch (OrderService.OperationNotAllowedException e) {
      return ResponseEntity.status(409).body(new ErrorResponse(e.getMessage()));
    }
  }

  @GetMapping("/products/{sku}")
  public ResponseEntity<?> getProduct(@PathVariable String sku) {
    try {
      OrderService.ProductDto product = orderService.getProduct(sku);
      ProductResponse response = new ProductResponse(
        product.sku(),
        product.name(),
        product.price(),
        product.weightKg(),
        product.stock(),
        product.discontinued()
      );
      return ResponseEntity.ok(response);
    } catch (OrderService.NotFoundException e) {
      return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
    }
  }

  @GetMapping("/customers/{cpf}")
  public ResponseEntity<?> getCustomer(@PathVariable String cpf) {
    try {
      OrderService.CustomerDto customer = orderService.getCustomer(cpf);
      CustomerResponse response = new CustomerResponse(
        customer.cpf(),
        customer.creditBalance()
      );
      return ResponseEntity.ok(response);
    } catch (OrderService.NotFoundException e) {
      return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
    }
  }

  @GetMapping("/customers/{cpf}/notifications")
  public ResponseEntity<?> getNotifications(@PathVariable String cpf) {
    try {
      List<NotificationResponse> notifications = notificationService.getNotifications(cpf);
      return ResponseEntity.ok(notifications);
    } catch (NotificationService.NotFoundException e) {
      return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
    }
  }

  @PostMapping("/notifications/{id}/read")
  public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id) {
    try {
      NotificationResponse notification = notificationService.markAsRead(id);
      return ResponseEntity.ok(notification);
    } catch (NotificationService.NotFoundException e) {
      return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
    }
  }
}
