package com.loja.checkout.payment;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentMethodRegistry {

    private final Map<String, PaymentMethod> formasPagamento;

    public PaymentMethodRegistry(List<PaymentMethod> formasPagamento) {
        this.formasPagamento = formasPagamento.stream()
                .collect(Collectors.toMap(PaymentMethod::codigo, Function.identity()));
    }

    public Optional<PaymentMethod> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(formasPagamento::get);
    }
}
