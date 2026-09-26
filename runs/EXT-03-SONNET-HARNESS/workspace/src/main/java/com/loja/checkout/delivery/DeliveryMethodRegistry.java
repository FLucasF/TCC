package com.loja.checkout.delivery;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DeliveryMethodRegistry {

    private final Map<String, DeliveryMethod> porCodigo;

    public DeliveryMethodRegistry(List<DeliveryMethod> metodos) {
        this.porCodigo = metodos.stream().collect(Collectors.toMap(DeliveryMethod::getCodigo, Function.identity()));
    }

    public Optional<DeliveryMethod> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
