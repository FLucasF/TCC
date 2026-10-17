package com.loja.orders.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.loja.orders.checkout.CheckoutRequestParser;
import com.loja.orders.common.JsonBodies;

import tools.jackson.databind.JsonNode;

@RestController
public class OrderController {

    private final CheckoutRequestParser parser;
    private final OrderService service;
    private final JsonBodies bodies;

    public OrderController(CheckoutRequestParser parser, OrderService service, JsonBodies bodies) {
        this.parser = parser;
        this.service = service;
        this.bodies = bodies;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse place(@RequestBody(required = false) String body) {
        return OrderResponse.of(service.place(parser.parse(body)));
    }

    @GetMapping("/orders/{id}")
    public OrderResponse get(@PathVariable String id) {
        return OrderResponse.of(service.get(id));
    }

    @PostMapping("/orders/{id}/payment")
    public OrderResponse pay(@PathVariable String id, @RequestBody(required = false) String body) {
        JsonNode json = bodies.read(body);
        return OrderResponse.of(service.registerPayment(id,
                JsonBodies.text(json, "transactionId"), JsonBodies.text(json, "paidAt")));
    }

    @PostMapping("/orders/{id}/shipment")
    public OrderResponse ship(@PathVariable String id, @RequestBody(required = false) String body) {
        return OrderResponse.of(service.ship(id, JsonBodies.text(bodies.read(body), "trackingCode")));
    }

    @PostMapping("/orders/{id}/delivery")
    public OrderResponse deliver(@PathVariable String id) {
        return OrderResponse.of(service.deliver(id));
    }

    @PostMapping("/orders/{id}/cancellation")
    public OrderResponse cancel(@PathVariable String id) {
        return OrderResponse.of(service.cancel(id));
    }
}
