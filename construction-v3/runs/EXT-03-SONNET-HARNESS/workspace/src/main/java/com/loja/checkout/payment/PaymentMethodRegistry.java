package com.loja.checkout.payment;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentMethodRegistry {

    private final Map<String, PaymentMethod> porCodigo;

    public PaymentMethodRegistry(List<PaymentMethod> metodos) {
        this.porCodigo = metodos.stream().collect(Collectors.toMap(PaymentMethod::getCodigo, Function.identity()));
    }

    public Optional<PaymentMethod> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
