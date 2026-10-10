package com.loja.checkout.delivery;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DeliveryOptionRegistry {

    private final Map<String, DeliveryOption> opcoes;

    public DeliveryOptionRegistry(List<DeliveryOption> opcoes) {
        this.opcoes = opcoes.stream()
                .collect(Collectors.toMap(DeliveryOption::codigo, Function.identity()));
    }

    public Optional<DeliveryOption> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(opcoes::get);
    }
}
