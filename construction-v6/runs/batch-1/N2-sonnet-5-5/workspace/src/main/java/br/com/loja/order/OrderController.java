package br.com.loja.order;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.checkout.CheckoutRequest;
import br.com.loja.checkout.OrderSummary;

@RestController
class OrderController {

    record OrderResponse(String id, OrderStatus status, Instant createdAt, OrderSummary summary,
                         List<OrderItem> items, String trackingCode) {
        static OrderResponse of(Order o) {
            return new OrderResponse(o.id(), o.status(), o.createdAt(), o.summary(), o.items(), o.trackingCode());
        }
    }

    record PaymentRequest(String transactionId, String paidAt) {
    }

    record ShipmentRequest(String trackingCode) {
    }

    private final OrderService orders;

    OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping("/orders")
    ResponseEntity<OrderResponse> create(@RequestBody(required = false) CheckoutRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.of(orders.create(request)));
    }

    @GetMapping("/orders/{id}")
    OrderResponse get(@PathVariable String id) {
        return OrderResponse.of(orders.get(id));
    }

    @PostMapping("/orders/{id}/payment")
    OrderResponse pay(@PathVariable String id, @RequestBody(required = false) PaymentRequest body) {
        return OrderResponse.of(orders.confirmPayment(id, body == null ? null : body.transactionId(),
                body == null ? null : body.paidAt()));
    }

    @PostMapping("/orders/{id}/shipment")
    OrderResponse ship(@PathVariable String id, @RequestBody(required = false) ShipmentRequest body) {
        return OrderResponse.of(orders.ship(id, body == null ? null : body.trackingCode()));
    }

    @PostMapping("/orders/{id}/delivery")
    OrderResponse deliver(@PathVariable String id) {
        return OrderResponse.of(orders.deliver(id));
    }

    @PostMapping("/orders/{id}/cancellation")
    OrderResponse cancel(@PathVariable String id) {
        return OrderResponse.of(orders.cancel(id));
    }
}
