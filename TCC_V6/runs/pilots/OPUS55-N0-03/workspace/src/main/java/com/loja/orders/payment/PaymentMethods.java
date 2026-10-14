package com.loja.orders.payment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class PaymentMethods {

    private final Map<String, PaymentMethod> byCode;

    public PaymentMethods(List<PaymentMethod> methods) {
        this.byCode = methods.stream().collect(Collectors.toMap(PaymentMethod::code, Function.identity()));
    }

    public Optional<PaymentMethod> find(String code) {
        return Optional.ofNullable(code).map(byCode::get);
    }
}
