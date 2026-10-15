package com.loja.checkout.domain.entrega;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ModalidadeEntregaRegistro {

    private final Map<String, ModalidadeEntregaStrategy> estrategias;

    public ModalidadeEntregaRegistro(List<ModalidadeEntregaStrategy> estrategias) {
        this.estrategias = estrategias.stream()
                .collect(Collectors.toMap(ModalidadeEntregaStrategy::codigo, Function.identity()));
    }

    public ModalidadeEntregaStrategy buscar(String codigo) {
        if (codigo == null) return null;
        return estrategias.get(codigo);
    }
}
