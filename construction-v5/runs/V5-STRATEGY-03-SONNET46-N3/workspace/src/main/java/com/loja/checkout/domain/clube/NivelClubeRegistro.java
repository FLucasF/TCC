package com.loja.checkout.domain.clube;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NivelClubeRegistro {

    private final Map<String, NivelClubeStrategy> estrategias;

    public NivelClubeRegistro(List<NivelClubeStrategy> estrategias) {
        this.estrategias = estrategias.stream()
                .collect(Collectors.toMap(NivelClubeStrategy::codigo, Function.identity()));
    }

    public NivelClubeStrategy buscar(String codigo) {
        if (codigo == null) return null;
        return estrategias.get(codigo);
    }
}
