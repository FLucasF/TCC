package br.com.loja.pedidos.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.pedidos.checkout.CheckoutRequest;
import br.com.loja.pedidos.checkout.CheckoutService;
import br.com.loja.pedidos.checkout.Summary;
import br.com.loja.pedidos.common.DomainException;
import br.com.loja.pedidos.order.Order;
import br.com.loja.pedidos.order.OrderService;

@RestController
class OrderController {

    record PaymentNotice(String transactionId, String paidAt) {
    }

    record Shipment(String trackingCode) {
    }

    private final CheckoutService checkout;
    private final OrderService orders;

    OrderController(CheckoutService checkout, OrderService orders) {
        this.checkout = checkout;
        this.orders = orders;
    }

    @PostMapping("/checkout/summary")
    Summary summary(@RequestBody(required = false) CheckoutRequest request) {
        return checkout.calculate(required(request));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    Order register(@RequestBody(required = false) CheckoutRequest request) {
        return orders.register(required(request));
    }

    @GetMapping("/orders/{id}")
    Order get(@PathVariable String id) {
        return orders.get(id);
    }

    @PostMapping("/orders/{id}/payment")
    Order payment(@PathVariable String id, @RequestBody(required = false) PaymentNotice notice) {
        return notice == null ? orders.confirmPayment(id, null, null)
                : orders.confirmPayment(id, notice.transactionId(), notice.paidAt());
    }

    @PostMapping("/orders/{id}/shipment")
    Order shipment(@PathVariable String id, @RequestBody(required = false) Shipment shipment) {
        return orders.ship(id, shipment == null ? null : shipment.trackingCode());
    }

    @PostMapping("/orders/{id}/delivery")
    Order delivery(@PathVariable String id) {
        return orders.deliver(id);
    }

    @PostMapping("/orders/{id}/cancellation")
    Order cancellation(@PathVariable String id) {
        return orders.cancel(id);
    }

    private static CheckoutRequest required(CheckoutRequest request) {
        if (request == null) {
            throw DomainException.unprocessable("INVALID_ORDER");
        }
        return request;
    }
}
