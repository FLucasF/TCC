package com.loja.payment;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class PaymentMethodRegistry {
    private final Map<String, PaymentMethod> methods;

    public PaymentMethodRegistry() {
        methods = new HashMap<>();
        methods.put("PIX", new PixPayment());
        methods.put("CARTAO", new CartaoPayment());
        methods.put("BOLETO", new BoletoPayment());
    }

    public PaymentMethod get(String code) {
        return methods.get(code);
    }
}
