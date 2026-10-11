package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MetodosEntregaRegistry {

    private final Map<String, MetodoEntrega> porCodigo;

    public MetodosEntregaRegistry(List<MetodoEntrega> metodos) {
        this.porCodigo = metodos.stream()
                .collect(Collectors.toMap(MetodoEntrega::codigo, Function.identity()));
    }

    public Optional<MetodoEntrega> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
