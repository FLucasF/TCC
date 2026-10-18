package br.com.loja.checkout;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CheckoutController {

    private final CheckoutService checkout;

    CheckoutController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    @PostMapping("/checkout/summary")
    OrderSummary summary(@RequestBody(required = false) CheckoutRequest request) {
        return checkout.summarize(request);
    }
}
