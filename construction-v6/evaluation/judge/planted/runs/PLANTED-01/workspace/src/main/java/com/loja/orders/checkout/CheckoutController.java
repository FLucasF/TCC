package com.loja.orders.checkout;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutRequestParser parser;
    private final CheckoutCalculator calculator;

    public CheckoutController(CheckoutRequestParser parser, CheckoutCalculator calculator) {
        this.parser = parser;
        this.calculator = calculator;
    }

    @PostMapping("/checkout/summary")
    public CheckoutSummary summary(@RequestBody(required = false) String body) {
        return calculator.calculate(parser.parse(body));
    }
}
