package br.com.loja.pedidos.api;

import br.com.loja.pedidos.checkout.CheckoutRequest;
import br.com.loja.pedidos.checkout.OrderSummary;
import br.com.loja.pedidos.order.Order;
import br.com.loja.pedidos.order.OrderService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OrderController {

    private final OrderService service;

    OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping("/checkout/summary")
    OrderSummary summary(@RequestBody(required = false) Map<String, Object> body) {
        return service.summarize(CheckoutRequest.parse(body));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    Order register(@RequestBody(required = false) Map<String, Object> body) {
        return service.register(CheckoutRequest.parse(body));
    }

    @GetMapping("/orders/{id}")
    Order get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping("/orders/{id}/payment")
    Order payment(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        return service.confirmPayment(id, text(body, "transactionId"), text(body, "paidAt"));
    }

    @PostMapping("/orders/{id}/shipment")
    Order shipment(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        return service.ship(id, text(body, "trackingCode"));
    }

    @PostMapping("/orders/{id}/delivery")
    Order delivery(@PathVariable String id) {
        return service.deliver(id);
    }

    @PostMapping("/orders/{id}/cancellation")
    Order cancellation(@PathVariable String id) {
        return service.cancel(id);
    }

    private static String text(Map<String, Object> body, String field) {
        return body != null && body.get(field) instanceof String s ? s : null;
    }
}
